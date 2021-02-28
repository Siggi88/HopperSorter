package io.siggi.hoppersorter;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class HopperSorter extends JavaPlugin implements Listener {
	private Set<Block> autoDroppers = new HashSet<>();

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getScheduler().runTaskTimer(this, this::autoDrop, 1L, 1L);
	}

	@EventHandler
	public void itemMove(InventoryMoveItemEvent event) {
		ItemStack item = event.getItem();
		Inventory source = event.getSource();
		Inventory destination = event.getDestination();
		if (isSortingHopper(source)) {
			int sameItem = 0;
			int total = 0;
			int slots = 0;
			for (ItemStack stack : source.getStorageContents()) {
				if (stack != null && stack.getType() != Material.AIR) {
					slots += 1;
					total += stack.getAmount();
					if (stack.isSimilar(item)) {
						sameItem += stack.getAmount();
					}
				}
			}
			if (sameItem <= 0) {
				event.setCancelled(true);
				if (total > slots) {
					getServer().getScheduler().runTask(this, () -> reorder(source));
				}
				return;
			}
		}
		if (isSortingHopper(destination)) {
			event.setCancelled(true);
			for (ItemStack stack : destination.getStorageContents()) {
				if (stack != null && stack.isSimilar(item)) {
					event.setCancelled(false);
					return;
				}
			}
		}
		if (isAutoDropper(destination)) {
			Container container = (Container) destination.getHolder();
			Block block = container.getBlock();
			autoDroppers.add(block);
		}
	}

	private void reorder(Inventory inventory) {
		ItemStack itemAt0 = inventory.getItem(0);
		for (int i = 1; i < inventory.getSize(); i++) {
			ItemStack item = inventory.getItem(i);
			if (item != null && item.getAmount() > 1) {
				inventory.setItem(i, itemAt0);
				inventory.setItem(0, item);
				return;
			}
		}
	}

	private boolean isSortingHopper(Inventory inventory) {
		if (inventory.getType() != InventoryType.HOPPER) {
			return false;
		}
		Hopper hopper = (Hopper) inventory.getHolder();
		String name = hopper.getCustomName();
		if (name == null) return false;
		String lowerName = ChatColor.stripColor(name).trim().replace(" ", "").toLowerCase();
		return lowerName.equals("sorter") || lowerName.equals("filter");
	}

	private boolean isAutoDropper(Inventory inventory) {
		if (inventory.getType() != InventoryType.DROPPER && inventory.getType() != InventoryType.DISPENSER) {
			return false;
		}
		Container container = (Container) inventory.getHolder();
		String name = container.getCustomName();
		if (name == null) return false;
		String lowerName = ChatColor.stripColor(name).trim().replace(" ", "").toLowerCase();
		return lowerName.equals("autodropper");
	}

	private void autoDrop() {
		for (Iterator<Block> it = autoDroppers.iterator(); it.hasNext(); ) {
			Block block = it.next();
			if (!block.getChunk().isLoaded()) {
				it.remove();
				continue;
			}
			BlockState state = block.getState();
			if (state instanceof Dispenser) {
				Dispenser dispenser = (Dispenser) state;
				if (dispenser.getInventory().isEmpty()) {
					it.remove();
					continue;
				}
				dispenser.dispense();
			} else if (state instanceof Dropper) {
				Dropper dropper = (Dropper) state;
				if (dropper.getInventory().isEmpty()) {
					it.remove();
					continue;
				}
				dropper.drop();
			} else {
				it.remove();
				continue;
			}
		}
	}
}
