package me.benfah.simpledrawers.api.drawer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import me.benfah.simpledrawers.api.drawer.holder.CombinedInventoryHandler;
import me.benfah.simpledrawers.api.drawer.holder.InventoryHandler;
import me.benfah.simpledrawers.api.drawer.holder.ItemHolder;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Tickable;

public abstract class BlockEntityAbstractDrawer extends BlockEntity implements BlockEntityClientSerializable, Tickable
{
		
	public BlockEntityAbstractDrawer(BlockEntityType<?> type)
	{
		super(type);
	}

	public abstract ItemHolder getItemHolderAt(float x, float y);

	public abstract List<ItemHolder> getItemHolders();

	protected abstract void setItemHolders(List<ItemHolder> holders);
	
	public abstract CombinedInventoryHandler getInventoryHandler();
	
	public void tick()
	{
		getItemHolders().forEach((holder) -> holder.getInventoryHandler().transferItems());
	}
	
	@Override
	public void sync()
	{
		markDirty();
		BlockEntityClientSerializable.super.sync();
	}
	
	@Override
	public void fromTag(CompoundTag tag)
	{
		fromClientTag(tag);
		super.fromTag(tag);
	}

	@Override
	public CompoundTag toTag(CompoundTag tag)
	{
		toClientTag(tag);
		return super.toTag(tag);
	}

	@Override
	public void fromClientTag(CompoundTag tag)
	{	
		List<ItemHolder> holders = new ArrayList<>();
		if(tag.contains("Holder"))
		{
			holders.add(ItemHolder.fromNBT(tag.getCompound("Holder"), this));
		}
		else if(tag.contains("Holders"))
		{
			ListTag listTag = tag.getList("Holders", 10);
			holders.addAll(listTag.stream()
			.map((holderTag) -> ItemHolder.fromNBT((CompoundTag) holderTag, this)).collect(Collectors.toList()));
		}
		if(!holders.isEmpty())
			setItemHolders(holders);
	}

	@Override
	public CompoundTag toClientTag(CompoundTag tag)
	{
		List<CompoundTag> holderList = getItemHolders().stream().map((holder) -> holder.toNBT(new CompoundTag()))
				.collect(Collectors.toList());
		ListTag listTag = new ListTag();
		listTag.addAll(holderList);
		tag.put("Holders", listTag);
		return tag;
	}

}
