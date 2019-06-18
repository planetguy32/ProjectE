package moze_intel.projecte.gameObjs.container;

import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.gameObjs.ObjHandler;
import moze_intel.projecte.gameObjs.container.slots.SlotGhost;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import moze_intel.projecte.gameObjs.tiles.CollectorMK1Tile;
import moze_intel.projecte.network.PacketHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;

public class CollectorMK1Container extends LongContainer
{
	final CollectorMK1Tile tile;
	public int sunLevel = 0;
	public long emc = 0;
	public double kleinChargeProgress = 0;
	public double fuelProgress = 0;
	public int kleinEmc = 0;

	public CollectorMK1Container(ContainerType<? extends CollectorMK1Container> type, int windowId, PlayerInventory invPlayer, CollectorMK1Tile collector)
	{
		super(type, windowId);
		this.tile = collector;
		initSlots(invPlayer);
	}

	void initSlots(PlayerInventory invPlayer)
	{
		IItemHandler aux = tile.getAux();
		IItemHandler main = tile.getInput();

		//Klein Star Slot
		this.addSlot(new ValidatedSlot(aux, CollectorMK1Tile.UPGRADING_SLOT, 124, 58, SlotPredicates.COLLECTOR_INV));

		int counter = main.getSlots() - 1;
		//Fuel Upgrade storage
		for (int i = 0; i <= 1; i++)
			for (int j = 0; j <= 3; j++)
				this.addSlot(new ValidatedSlot(main, counter--, 20 + i * 18, 8 + j * 18, SlotPredicates.COLLECTOR_INV));

		//Upgrade Result
		this.addSlot(new ValidatedSlot(aux, CollectorMK1Tile.UPGRADE_SLOT, 124, 13, SlotPredicates.COLLECTOR_INV));

		//Upgrade Target
		this.addSlot(new SlotGhost(aux, CollectorMK1Tile.LOCK_SLOT, 153, 36, SlotPredicates.COLLECTOR_LOCK));

		//Player inventory
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 9; j++)
				this.addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));

		//Player hotbar
		for (int i = 0; i < 9; i++)
			this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 142));
	}

	@Override
	public void addListener(IContainerListener listener)
	{
		super.addListener(listener);
		PacketHandler.sendProgressBarUpdateInt(listener, this, 0, tile.getSunLevel());
		PacketHandler.sendProgressBarUpdateLong(listener, this, 1, (long) tile.getStoredEmc());
		PacketHandler.sendProgressBarUpdateInt(listener, this, 2, (int) (tile.getItemChargeProportion() * 8000));
		PacketHandler.sendProgressBarUpdateInt(listener, this, 3, (int) (tile.getFuelProgress() * 8000));
		PacketHandler.sendProgressBarUpdateInt(listener, this, 4, (int) tile.getItemCharge());
	}

	@Nonnull
	@Override
	public ItemStack slotClick(int slot, int button, ClickType flag, PlayerEntity player)
	{
		if (slot >= 0 && getSlot(slot) instanceof SlotGhost && !getSlot(slot).getStack().isEmpty())
		{
			getSlot(slot).putStack(ItemStack.EMPTY);
			return ItemStack.EMPTY;
		} else
		{
			return super.slotClick(slot, button, flag, player);
		}
	}

	@Override
	public void detectAndSendChanges()
	{
		super.detectAndSendChanges();

		if (sunLevel != tile.getSunLevel())
		{
			for (IContainerListener icrafting : this.listeners)
			{
				PacketHandler.sendProgressBarUpdateInt(icrafting, this, 0, tile.getSunLevel());
			}

			sunLevel = tile.getSunLevel();
		}

		if (emc != ((long) tile.getStoredEmc()))
		{
			for (IContainerListener icrafting : this.listeners)
			{
				PacketHandler.sendProgressBarUpdateLong(icrafting, this, 1, ((long) tile.getStoredEmc()));
			}

			emc = ((long) tile.getStoredEmc());
		}

		if (kleinChargeProgress != tile.getItemChargeProportion())
		{
			for (IContainerListener icrafting : this.listeners)
			{
				PacketHandler.sendProgressBarUpdateInt(icrafting, this, 2, (int) (tile.getItemChargeProportion() * 8000));
			}

			kleinChargeProgress = tile.getItemChargeProportion();
		}

		if (fuelProgress != tile.getFuelProgress())
		{
			for (IContainerListener icrafting : this.listeners)
			{
				PacketHandler.sendProgressBarUpdateInt(icrafting, this, 3, (int) (tile.getFuelProgress() * 8000));
			}

			fuelProgress = tile.getFuelProgress();
		}

		if (kleinEmc != ((int) tile.getItemCharge()))
		{
			for (IContainerListener icrafting : this.listeners)
			{
				PacketHandler.sendProgressBarUpdateInt(icrafting, this, 4, (int) (tile.getItemCharge()));
			}

			kleinEmc = ((int) tile.getItemCharge());
		}

	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void updateProgressBar(int id, int data)
	{
		switch (id)
		{
			case 0: sunLevel = data; break;
			case 1: emc = data; break;
			case 2: kleinChargeProgress = data / 8000.0; break;
			case 3: fuelProgress = data / 8000.0; break;
			case 4: kleinEmc = data; break;
		}
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void updateProgressBarLong(int id, long data)
	{
		switch (id)
		{
			case 1: emc = data; break;
			default: updateProgressBar(id, (int) data);
		}
	}

	@Nonnull
	@Override
	public ItemStack transferStackInSlot(PlayerEntity player, int slotIndex)
	{
		Slot slot = this.getSlot(slotIndex);
		
		if (slot == null || !slot.getHasStack()) 
		{
			return ItemStack.EMPTY;
		}
		
		ItemStack stack = slot.getStack();
		ItemStack newStack = stack.copy();
		
		if (slotIndex <= 10)
		{
			if (!this.mergeItemStack(stack, 11, 46, false))
			{
				return ItemStack.EMPTY;
			}
		}
		else if (slotIndex <= 46)
		{
			if (!FuelMapper.isStackFuel(stack) || FuelMapper.isStackMaxFuel(stack) || !this.mergeItemStack(stack, 1, 8, false))
			{
				return ItemStack.EMPTY;
			}
		}
		else
		{
			return ItemStack.EMPTY;
		}
		
		if (stack.isEmpty())
		{
			slot.putStack(ItemStack.EMPTY);
		}
		else
		{
			slot.onSlotChanged();
		}
		
		return slot.onTake(player, stack);
	}

	@Override
	public boolean canInteractWith(@Nonnull PlayerEntity player)
	{
		return player.world.getBlockState(tile.getPos()).getBlock() == ObjHandler.collectorMK1
			&& player.getDistanceSq(tile.getPos().getX() + 0.5, tile.getPos().getY() + 0.5, tile.getPos().getZ() + 0.5) <= 64.0;
	}
}
