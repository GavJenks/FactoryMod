package com.github.igotyou.FactoryMod.listeners;

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.ItemMeta;

import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.ReinforcementManager;

import com.github.igotyou.FactoryMod.FactoryModManager;
import com.github.igotyou.FactoryMod.factories.Factory;
import com.github.igotyou.FactoryMod.structures.MultiBlockStructure;

public class FactoryModListener implements Listener {
	private FactoryModManager manager;
	private ReinforcementManager rm;

	public FactoryModListener(FactoryModManager manager) {
		this.manager = manager;
		if (manager.isCitadelEnabled()) {
			rm = Citadel.getReinforcementManager();
		}
	}

	/**
	 * Called when a block is broken If the block that is destroyed is part of a
	 * factory, call the required methods.
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void blockBreakEvent(BlockBreakEvent e) {
		Block block = e.getBlock();
		if (manager.isPossibleInteractionBlock(block.getType())) {
			Factory c = manager.getFactoryAt(block);
			if (c != null) {
				//let creative player interact without breaking it
				if (e.getPlayer().getGameMode() == GameMode.CREATIVE && e.getPlayer().getInventory().getItemInMainHand() != null 
						&& e.getPlayer().getInventory().getItemInMainHand().getType() == manager.getFactoryInteractionMaterial()) {
					e.setCancelled(true);
					return;
				}
				c.getInteractionManager().blockBreak(e.getPlayer(), block);
			}
		}

	}

	@EventHandler
	public void redstoneChange(BlockRedstoneEvent e) {
		if (e.getOldCurrent() == e.getNewCurrent()) {
			return;
		}
		for (BlockFace face : MultiBlockStructure.allBlockSides) {
			Factory f = manager.getFactoryAt(e.getBlock().getRelative(face));
			if (f != null) {
				f.getInteractionManager().redStoneEvent(e,
						e.getBlock().getRelative(face));
			}
		}
	}

	/**
	 * Called when a entity explodes(creeper,tnt etc.) Nearly the same as
	 * blockBreakEvent
	 */
	@EventHandler
	public void explosionListener(EntityExplodeEvent e) {
		List<Block> blocks = e.blockList();
		for (Block block : blocks) {
			if (manager.isPossibleInteractionBlock(block.getType())
					&& ((manager.isCitadelEnabled() && !rm.isReinforced(block)) || !manager
							.isCitadelEnabled())) {
				Factory c = manager.getFactoryAt(block);
				if (c != null) {
					c.getInteractionManager().blockBreak(null, block);
				}
			}
		}
	}

	/**
	 * Called when a block burns Nearly the same as blockBreakEvent
	 */
	@EventHandler
	public void burnListener(BlockBurnEvent e) {
		Block block = e.getBlock();
		if (manager.isPossibleInteractionBlock(block.getType())
				&& ((manager.isCitadelEnabled() && !rm.isReinforced(block)) || !manager
						.isCitadelEnabled())) {
			Factory c = manager.getFactoryAt(block);
			if (c != null) {
				c.getInteractionManager().blockBreak(null, block);
			}
		}
	}

	@EventHandler
	public void playerInteract(PlayerInteractEvent e) {
		Block block = e.getClickedBlock();
		Player player = e.getPlayer();
		if (block != null
				&& manager.isPossibleInteractionBlock(block.getType())) {
			BlockFace bf = e.getBlockFace();
			Factory c = manager.getFactoryAt(block);
			if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (c != null) {
					c.getInteractionManager().rightClick(player, block, bf);
				} else {
					// check if chest is other half of double chest
					if (block.getType() == Material.CHEST) {
						for (Block b : MultiBlockStructure
								.searchForBlockOnSides(block, Material.CHEST)) {
							Factory f = manager.getFactoryAt(b);
							if (f != null) {
								f.getInteractionManager().rightClick(player, b,
										bf);
							}
						}
					}
				}
			}
			if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (c == null) {
					if (manager.isPossibleCenterBlock(block.getType())) {
						if (player.getInventory().getItemInMainHand().getType() == manager
								.getFactoryInteractionMaterial()) {
							manager.attemptCreation(block, player);
						}
					} else {
						// check if chest is other half of double chest
						if (block.getType() == Material.CHEST) {
							for (Block b : MultiBlockStructure
									.searchForBlockOnAllSides(block,
											Material.CHEST)) {
								Factory f = manager.getFactoryAt(b);
								if (f != null) {
									f.getInteractionManager().leftClick(player,
											b, bf);
								}
							}
						}
					}
				} else {
					c.getInteractionManager().leftClick(player, block, bf);
				}

			}
		}
	}

	@EventHandler
	public void blockDispenser(BlockDispenseEvent e) {
		if (manager.getFactoryAt(e.getBlock()) != null) {
			e.setCancelled(true);
		}
	}

	/**
	 * Turns slabs with the lore "Smooth double slab" into smooth double slab
	 * blocks and logs with the lore "6-sided log" into logs with the log
	 * texture on all 6 sides
	 * Turns mushroom blocks with the lore "Mushroom stem" into blocks with the 
	 * stem textures on all sides and mushroom blocks with the lore "Mushroom pores"
	 * into blocks with pores texture on all sides
	 * 
	 * @param e
	 */
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onSpecialBlockUse(BlockPlaceEvent e) {
		org.bukkit.inventory.ItemStack is = e.getItemInHand();
		if (!is.hasItemMeta() || !is.getItemMeta().hasLore()) {
			return;
		}
		Material material = e.getBlock().getType();
		ItemMeta blockMeta = is.getItemMeta();
		switch (material) {
		case STEP:
			if (blockMeta.getLore().get(0).equals("Smooth double slab")) {
				byte type = (byte) (is.getDurability() + 8);
				e.getBlock().setTypeIdAndData(Material.DOUBLE_STEP.getId(),
						type, true);
			}
			break;
		case LOG:
		case LOG_2:
			if (blockMeta.getLore().get(0).equals("Sixsided log")) {
				byte type = (byte) ((is.getDurability() % 4) + 12);
				e.getBlock().setTypeIdAndData(material.getId(), type, true);
			}

			break;
		case HUGE_MUSHROOM_1:
		case HUGE_MUSHROOM_2:
			if (blockMeta.getLore().get(0).equals("Mushroom stem")) {
				byte type = (byte) 15;
				e.getBlock().setTypeIdAndData(material.getId(), type, true);
			}
			if (blockMeta.getLore().get(0).equals("Mushroom pores")) {
				byte type = (byte) 0;
				e.getBlock().setTypeIdAndData(material.getId(), type, true);
			}
			break;
		default:
			return;
		}

	}
}
