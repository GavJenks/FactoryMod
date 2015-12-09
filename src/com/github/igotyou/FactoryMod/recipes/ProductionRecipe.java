package com.github.igotyou.FactoryMod.recipes;

import java.util.LinkedList;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.github.igotyou.FactoryMod.utility.ItemMap;

public class ProductionRecipe implements IRecipe {
	private String recipeName;
	private int productionTime;
	private ItemMap inputs;
	private ItemMap outputs;
	private ItemStack itemRepresentation;

	public ProductionRecipe(String recipeName, int productionTime,
			ItemMap inputs, ItemMap outputs) {
		this.recipeName = recipeName;
		this.productionTime = productionTime;
		this.inputs = inputs;
		this.outputs = outputs;
		LinkedList <ItemStack> outputRepresentation = outputs.getItemStackRepresentation();
		if (outputRepresentation.size() != 0) {
			itemRepresentation = outputRepresentation.get(0);
		}
		else {
			itemRepresentation = new ItemStack(Material.STONE);
		}
	}

	public ItemMap getRawInputs() {
		return inputs;
	}

	public ItemMap getRawOutputs() {
		return outputs;
	}

	public String getRecipeName() {
		return recipeName;
	}

	public int getProductionTime() {
		return productionTime;
	}
	
	public int getCurrentMultiplier(Inventory i) {
		ItemMap im = new ItemMap(i);
		return inputs.getMultiplesContainedIn(im);
	}
	
	public ItemMap getCurrentOutput(Inventory i) {
		ItemMap copy = outputs.clone();
		copy.multiplyContent(getCurrentMultiplier(i));
		return copy;
	}	
	
	public ItemStack getItemRepresentation() {
		return itemRepresentation;
	}
}
