package bspkrs.ingameinfo.fml;

import java.util.EnumSet;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import bspkrs.bspkrscore.fml.bspkrsCoreMod;
import bspkrs.ingameinfo.CommandIGI;
import bspkrs.ingameinfo.IngameInfo;
import bspkrs.util.Const;
import bspkrs.util.ModVersionChecker;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.Metadata;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

@Mod(name = "IngameInfo", modid = "IngameInfo", version = "Forge " + IngameInfo.VERSION_NUMBER, dependencies = "required-after:bspkrsCore", useMetadata = true)
public class IngameInfoMod
{
    public static ModVersionChecker versionChecker;
    private String                  versionURL      = Const.VERSION_URL + "/Minecraft/" + Const.MCVERSION + "/ingameInfoForge.version";
    private String                  mcfTopic        = "http://www.minecraftforum.net/topic/1009577-";
    
    private boolean                 registerCommand = false;
    
    @Metadata(value = "IngameInfo")
    public static ModMetadata       metadata;
    
    @Instance(value = "IngameInfo")
    public static IngameInfoMod     instance;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        metadata = event.getModMetadata();
        IngameInfo.loadConfig(event.getSuggestedConfigurationFile());
        
        if (bspkrsCoreMod.instance.allowUpdateCheck)
        {
            versionChecker = new ModVersionChecker(metadata.name, metadata.version, versionURL, mcfTopic);
            versionChecker.checkVersionWithLogging();
        }
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        KeyBinding[] keys = { IngameInfo.toggleKey };
        boolean[] repeats = { false };
        KeyBindingRegistry.registerKeyBinding(new IGIKeyHandler(keys, repeats));
        
        TickRegistry.registerTickHandler(new IGIGameTicker(EnumSet.of(TickType.CLIENT)), Side.CLIENT);
        TickRegistry.registerTickHandler(new IGIRenderTicker(EnumSet.of(TickType.RENDER)), Side.CLIENT);
        
        try
        {
            ClientCommandHandler.instance.registerCommand(new CommandIGI());
        }
        catch (Throwable e)
        {
            registerCommand = true;
        }
    }
    
    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        if (registerCommand)
            event.registerServerCommand(new CommandIGI());
    }
}
