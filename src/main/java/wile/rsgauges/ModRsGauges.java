/*
 * @file ModRsGauges.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Main mod class.
 */
package wile.rsgauges;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wile.rsgauges.detail.BlockCategories;
import wile.rsgauges.libmc.detail.*;

import java.util.List;


@Mod("rsgauges")
public class ModRsGauges
{
  public static final String MODID = "rsgauges";
  public static final String MODNAME = "Gauges and Switches";
  public static final int VERSION_DATAFIXER = 0;
  private static final Logger LOGGER = LogManager.getLogger();

  // -------------------------------------------------------------------------------------------------------------------

  public ModRsGauges()
  {
    Auxiliaries.init(MODID, LOGGER, ModConfig::getServerConfig);
    Auxiliaries.logGitVersion(MODNAME);
    Registries.init(MODID, "industrial_small_lever");
    ModContent.init(MODID);
    OptionalRecipeCondition.init(MODID, LOGGER);
    ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_CONFIG_SPEC);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(ForgeEvents::onSetup);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(ForgeEvents::onClientSetup);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addCreative);
    MinecraftForge.EVENT_BUS.register(this);
    PlayerBlockInteraction.init(MODID, LOGGER);
  }

  private void addCreative(BuildCreativeModeTabContentsEvent event)
  {
    if(event.getTab() == Registries.RSGAUGES_TAB.get())
    {
      List<Block> blocks = Registries.getRegisteredBlocks();
      List<Item> items = Registries.getRegisteredItems();

      blocks.forEach((b) -> event.accept(b));
      items.forEach((i) -> event.accept(i));
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Events
  // -------------------------------------------------------------------------------------------------------------------

  @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
  public static final class ForgeEvents
  {
    public static void onSetup(final FMLCommonSetupEvent event)
    {
      CraftingHelper.register(OptionalRecipeCondition.Serializer.INSTANCE);
      wile.rsgauges.libmc.detail.Networking.init(MODID);
      BlockCategories.update();
    }

    public static void onClientSetup(final FMLClientSetupEvent event)
    {
      Overlay.register();
      ModContent.processContentClientSide(event);
    }

    @SubscribeEvent
    public static void onConfigLoad(final ModConfigEvent.Loading event)
    { ModConfig.apply(); }

    @SubscribeEvent
    public static void onConfigReload(final ModConfigEvent.Reloading event)
    {
      try {
        Auxiliaries.logger().info("Config file changed {}", event.getConfig().getFileName());
        ModConfig.apply();
      } catch(Throwable e) {
        Auxiliaries.logger().error("Failed to load changed config: " + e.getMessage());
      }
    }
  }
}
