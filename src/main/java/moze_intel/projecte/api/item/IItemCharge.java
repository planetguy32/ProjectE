package moze_intel.projecte.api.item;

import moze_intel.projecte.api.PESounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This interface specifies items that have a charge that changes when the respective keybinding is activated (default V)
 */
public interface IItemCharge 
{
	String KEY = "Charge";

	int getNumCharges(@Nonnull ItemStack stack);
	/**
	 * Returns the current charge on the given ItemStack
	 * @param stack Stack whose charge we want
	 * @return The charge on the stack
	 */
	default int getCharge(@Nonnull ItemStack stack) {
		if (!stack.hasTag())
		{
			stack.setTag(new CompoundNBT());
		}

		return stack.getTag().getInt(KEY);
	}

	/**
	 * Called serverside when the player presses the charge keybinding; reading sneaking state is up to you
	 * @param player The player
	 * @param stack The item being charged
	 * @param hand The hand this stack was in, or null if the call was not from the player's hands
	 * @return Whether the operation succeeded
	 */
	default boolean changeCharge(@Nonnull PlayerEntity player, @Nonnull ItemStack stack, @Nullable Hand hand) {
		int currentCharge = getCharge(stack);
		int numCharges = getNumCharges(stack);

		if (player.isSneaking())
		{
			if (currentCharge > 0)
			{
				player.getEntityWorld().playSound(null, player.posX, player.posY, player.posZ, PESounds.UNCHARGE, SoundCategory.PLAYERS, 1.0F, 0.5F + ((0.5F / (float)numCharges) * currentCharge));
				stack.getTag().putInt(KEY, currentCharge - 1);
				return true;
			}
		}
		else if (currentCharge < numCharges)
		{
			player.getEntityWorld().playSound(null, player.posX, player.posY, player.posZ, PESounds.CHARGE, SoundCategory.PLAYERS, 1.0F, 0.5F + ((0.5F / (float)numCharges) * currentCharge));
			stack.getTag().putInt(KEY, currentCharge + 1);
			return true;
		}

		return false;
	}
}
