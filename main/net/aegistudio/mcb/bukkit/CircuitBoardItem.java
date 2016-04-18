package net.aegistudio.mcb.bukkit;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;

import net.aegistudio.mpp.export.PluginCanvasRegistry;

public class CircuitBoardItem implements Listener {
	public final MapCircuitBoard plugin;
	public CircuitBoardItem(MapCircuitBoard plugin) {
		this.plugin = plugin;
	}
	
	public Material boardMaterial = Material.PAINTING;
	
	public String loreString = ChatColor.RESET + "Circuit Board Item";
	public String idString = ChatColor.MAGIC + "===========";
	
	public void make(ItemStack item, PluginCanvasRegistry<SchemeCanvas> canvas) {
		item.setType(boardMaterial);
		
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(canvas.name());
		
		ArrayList<String> lore = new ArrayList<String>();
		lore.add(loreString);
		lore.add(idString + canvas.mapid());
		meta.setLore(lore);
		
		meta.addEnchant(Enchantment.DURABILITY, 0, false);
		
		item.setItemMeta(meta);
	}
	
	public PluginCanvasRegistry<SchemeCanvas> parse(ItemStack item) {
		if(item == null) return null;
		if(item.getType() != boardMaterial) return null;
		List<String> lores = item.getItemMeta().getLore();
		if(lores.size() < 2) return null;
		if(!lores.get(0).equals(loreString)) return null;
		return plugin.schemes.get(Integer.parseInt(lores.get(1)
				.substring(idString.length())));
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onItemUse(PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		if(block == null) return;
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		
		PluginCanvasRegistry<SchemeCanvas> scheme = parse(event.getItem());
		if(scheme == null) return;
		
		PluginCanvasRegistry<CircuitBoardCanvas> board = null;
		if(plugin.circuit.isEmpty()) try {
			board = plugin.canvasService.generate(plugin, "redstone", CircuitBoardCanvas.class);
			MapView map = plugin.getServer().createMap(event.getPlayer().getWorld());
			plugin.canvasService.create(map.getId(), null, 
					"circuitboard_" + map.getId(), board);
		}
		catch(Exception e) {
			board = null;
		}
		else board = plugin.circuit.remove(plugin.circuit.firstKey());
		if(board == null || board.mapid() < 0) return;
		
		Location blockLocation = block.getLocation();
		try {
			ItemFrame frame = blockLocation.getWorld().spawn(blockLocation.add(
					event.getBlockFace().getModX(),
					event.getBlockFace().getModY(),
					event.getBlockFace().getModZ()), ItemFrame.class);
			frame.setFacingDirection(event.getBlockFace());
			frame.setItem(new ItemStack(Material.MAP, 1, (short)board.mapid()));
			
			final PluginCanvasRegistry<CircuitBoardCanvas> actualBoard = board;
			plugin.getServer().getScheduler().runTaskLater(plugin, 
					() -> actualBoard.canvas().refer(block.getLocation(), scheme), 1);
			
			if(event.getPlayer().getGameMode() == GameMode.SURVIVAL)
				event.getPlayer().getItemInHand().setAmount(event.getItem().getAmount() - 1);
		}
		catch(Throwable e) {
			
		}
		event.setCancelled(true);
	}
}
