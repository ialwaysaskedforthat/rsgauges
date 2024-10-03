/*
 * @file LinkReceiverSwitchBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Switch-Link receiver switches.
 */
package wile.rsgauges.blocks;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import wile.rsgauges.detail.ModResources;
import wile.rsgauges.detail.SwitchLink;
import wile.rsgauges.detail.SwitchLink.RequestResult;

import javax.annotation.Nullable;


public class LinkReceiverSwitchBlock extends SwitchBlock
{
  private final boolean is_analog;

  public LinkReceiverSwitchBlock(long config, BlockBehaviour.Properties properties, AABB unrotatedBBUnpowered, @Nullable AABB unrotatedBBPowered, @Nullable ModResources.BlockSoundEvent powerOnSound, @Nullable ModResources.BlockSoundEvent powerOffSound, boolean analog_device)
  { super(config, properties, unrotatedBBUnpowered, unrotatedBBPowered, powerOnSound, powerOffSound); is_analog = analog_device; }

  @Override
  public boolean switchLinkHasAnalogSupport(Level world, BlockPos pos)
  { return is_analog; }

  @Override
  public ImmutableList<SwitchLink.LinkMode> switchLinkGetSupportedTargetModes()
  { return (!is_analog) ? super.switchLinkGetSupportedTargetModes() : ImmutableList.of(SwitchLink.LinkMode.AS_STATE, SwitchLink.LinkMode.INV_STATE); }

  @Override
  public void switchLinkInit(SwitchLink link)
  { if(!is_analog) super.switchLinkInit(link); }

  @Override
  public SwitchLink.RequestResult switchLinkTrigger(SwitchLink link)
  {
    BlockPos pos = link.target_position;
    Level world = link.world;
    SwitchTileEntity te = getTe(world, pos);
    BlockState state = world.getBlockState(pos);
    if((te==null) || (!te.verifySwitchLinkTarget(link))) return RequestResult.TARGET_GONE;
    int p = is_analog ? link.source_analog_power : link.source_digital_power;
    final boolean was_powered = state.getValue(POWERED);
    if((!is_analog) && (link.mode() != SwitchLink.LinkMode.AS_STATE)) p = was_powered ? 0:15;
    final boolean powered = (p>0);
    if(powered) te.setpower(p);
    if(powered != was_powered) {
      if((config & SWITCH_CONFIG_PULSE)==0) {
        world.setBlock(pos, state.setValue(POWERED, powered), 1|2|8|16);
        (powered ? power_off_sound : power_on_sound).play(world, pos);
      } else {
        if(powered) {
          world.setBlock(pos, state.setValue(POWERED, true), 1|2|8|16);
          power_on_sound.play(world, pos);
//          if (!ModConfig.without_sculk_triggering && (power_on_sound.volume() >= ModConfig.sculk_trigger_threshold)) {
//            world.gameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Context.of(state));
//          }
          te.on_timer_reset();
          te.on_timer_extend();
          te.reschedule_block_tick();
        }
      }
    }
    notifyNeighbours(world, pos, state, te, false);
    if(!te.activateSwitchLinks(te.setpower(), powered?15:0, powered != was_powered)) {
      ModResources.BlockSoundEvents.SWITCHLINK_LINK_PEAL_USE_FAILED.play(world, pos);
    }
    return SwitchLink.RequestResult.OK;
  }
}
