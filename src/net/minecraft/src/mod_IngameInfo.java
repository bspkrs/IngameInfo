package net.minecraft.src;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.logging.Level;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.EnumGameType;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import org.lwjgl.opengl.GL11;

import bspkrs.client.util.HUDUtils;
import bspkrs.util.BSProp;
import bspkrs.util.BSPropRegistry;
import bspkrs.util.BlockID;
import bspkrs.util.CommonUtils;
import bspkrs.util.Const;
import bspkrs.util.Coord;
import bspkrs.util.ForgeUtils;
import bspkrs.util.ModVersionChecker;

public class mod_IngameInfo extends BaseMod
{
    private final Minecraft   mc;
    private ScaledResolution  scaledResolution;
    private int               alignMode;
    int                       rowNum[];
    int                       rowCount[];
    String                    text[];
    String                    fileName;
    private boolean           enabled       = true;
    private boolean           isForgeEnv    = false;
    
    @BSProp(info = "Valid memory unit strings are KB, MB, GB")
    public static String      memoryUnit    = "MB";
    @BSProp(info = "Horizontal offsets from the edge of the screen (when using right alignments the x offset is relative to the right edge of the screen)")
    public static String      xOffsets      = "2, 0, 2, 2, 0, 2, 2, 0, 2";
    public static int[]       xOffset;
    @BSProp(info = "Vertical offsets for each alignment position starting at top left (when using bottom alignments the y offset is relative to the bottom edge of the screen)")
    public static String      yOffsets      = "2, 2, 2, 0, 0, 0, 2, 41, 2";
    public static int[]       yOffset;
    @BSProp(info = "Set to true to show info when chat is open, false to disable info when chat is open\n\n**ONLY EDIT WHAT IS BELOW THIS**")
    public static boolean     showInChat    = false;
    private final String[]    defaultConfig = { "<topleft>&fDay <day> (<daytime[&e/&8]><mctime[12]>&f) <slimes[<darkgreen>/&b]><biome>", "Light: <max[<lightnosunfeet>/7[&e/&c]]><max[<lightnosunfeet>/9[&a/]]><lightnosunfeet>", "&fXP: &e<xpthislevel>&f / &e<xpcap>", "Time: &b<rltime[h:mma]>" };
    private final String      configPath;
    
    private ModVersionChecker versionChecker;
    private boolean           allowUpdateCheck;
    private final String      versionURL    = Const.VERSION_URL + "/Minecraft/" + Const.MCVERSION + "/ingameInfo.version";
    private final String      mcfTopic      = "http://www.minecraftforum.net/topic/1009577-";
    
    @Override
    public String getName()
    {
        return "IngameInfo";
    }
    
    @Override
    public String getVersion()
    {
        return "ML " + Const.MCVERSION + ".r04";
    }
    
    @Override
    public String getPriorities()
    {
        return "required-after:mod_bspkrsCore";
    }
    
    public mod_IngameInfo()
    {
        BSPropRegistry.registerPropHandler(this.getClass());
        mc = ModLoader.getMinecraftInstance();
        configPath = "/config/IngameInfo/";
        fileName = "ingameInfo.txt";
    }
    
    public void loadFormatFile()
    {
        alignMode = 0;
        rowCount = (new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 });
        xOffset = (new int[] { 2, 0, 2, 2, 0, 2, 2, 0, 2 });
        yOffset = (new int[] { 2, 2, 2, 0, 0, 0, 2, 41, 2 });
        text = loadText(new File(CommonUtils.getMinecraftDir(), configPath + fileName));
        for (int i = 0; i < text.length; i++)
        {
            if (text[i].toLowerCase().contains("<left>") || text[i].toLowerCase().contains("<topleft>"))
                alignMode = 0;
            else if (text[i].toLowerCase().contains("<center>") || text[i].toLowerCase().contains("<topcenter>"))
                alignMode = 1;
            else if (text[i].toLowerCase().contains("<right>") || text[i].toLowerCase().contains("<topright>"))
                alignMode = 2;
            else if (text[i].toLowerCase().contains("<middleleft>"))
                alignMode = 3;
            else if (text[i].toLowerCase().contains("<middlecenter>"))
                alignMode = 4;
            else if (text[i].toLowerCase().contains("<middleright>"))
                alignMode = 5;
            else if (text[i].toLowerCase().contains("<bottomleft>"))
                alignMode = 6;
            else if (text[i].toLowerCase().contains("<bottomcenter>"))
                alignMode = 7;
            else if (text[i].toLowerCase().contains("<bottomright>"))
                alignMode = 8;
            
            rowCount[alignMode]++;
        }
        
        String[] o = xOffsets.split(",");
        for (int i = 0; i < o.length; i++)
            xOffset[i] = CommonUtils.parseInt(o[i].trim());
        
        o = yOffsets.split(",");
        for (int i = 0; i < o.length; i++)
            yOffset[i] = CommonUtils.parseInt(o[i].trim());
    }
    
    @Override
    public void load()
    {
        loadFormatFile();
        
        ModLoader.addCommand(new CommandIGI());
        ModLoader.addLocalization("commands.igi.usage", "igi [reload/enable/disable/toggle]");
        
        allowUpdateCheck = mod_bspkrsCore.allowUpdateCheck;
        if (allowUpdateCheck)
        {
            versionChecker = new ModVersionChecker(getName(), getVersion(), versionURL, mcfTopic);
            versionChecker.checkVersionWithLogging();
        }
        
        ModLoader.setInGameHook(this, true, false);
        
        isForgeEnv = ForgeUtils.isForgeEnv();
    }
    
    @Override
    public boolean onTickInGame(float f, Minecraft mc)
    {
        if (enabled && (mc.inGameHasFocus || mc.currentScreen == null || (mc.currentScreen instanceof GuiChat && showInChat)) && !mc.gameSettings.showDebugInfo)
        {
            scaledResolution = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
            rowNum = (new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 });
            String lines[] = text;
            int i = lines.length;
            
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            //mc.renderEngine.resetBoundTexture();
            
            for (int j = 0; j < i; j++)
            {
                try
                {
                    String s = lines[j];
                    s = replaceAllTags(s);
                    // if(!(alignMode == 6 && mc.currentScreen != null &&
                    // mc.currentScreen instanceof GuiChat)) //if chat is open, don't display bottomleft info
                    mc.fontRenderer.drawStringWithShadow(s, getX(mc.fontRenderer.getStringWidth(HUDUtils.stripCtrl(s))), getY(rowCount[alignMode], rowNum[alignMode]), 0xffffff);
                    rowNum[alignMode]++;
                }
                catch (Throwable e)
                {
                    mc.thePlayer.addChatMessage(String.format("IngameInfo encountered an exception parsing ingameInfo.txt. Check %s for details.", CommonUtils.getLogFileName()));
                    e.printStackTrace();
                    enabled = false;
                }
            }
            
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        }
        
        if (allowUpdateCheck && versionChecker != null)
        {
            if (!versionChecker.isCurrentVersion())
                for (String msg : versionChecker.getInGameMessage())
                    mc.thePlayer.addChatMessage(msg);
            allowUpdateCheck = false;
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        }
        
        return true;
    }
    
    private String[] loadText(File file)
    {
        ArrayList arraylist = new ArrayList();
        Scanner scanner = null;
        if (!file.exists())
        {
            file = createFile();
        }
        try
        {
            scanner = new Scanner(file);
        }
        catch (Throwable e)
        {
            ModLoader.getLogger().log(Level.WARNING, "Error getting ingameInfo.txt: " + e.getMessage());
            e.printStackTrace();
            if (scanner != null)
                scanner.close();
            
            return new String[] { "" };
        }
        while (scanner.hasNextLine())
        {
            arraylist.add(scanner.nextLine());
        }
        scanner.close();
        return (String[]) arraylist.toArray(new String[arraylist.size()]);
    }
    
    private File createFile()
    {
        File file = new File(CommonUtils.getMinecraftDir(), configPath);
        
        if (!file.exists())
            file.mkdir();
        
        file = new File(file, fileName);
        try
        {
            file.createNewFile();
            PrintWriter out = new PrintWriter(new FileWriter(file));
            
            for (String s : defaultConfig)
                out.println(s);
            
            out.close();
            
            return file;
        }
        catch (Throwable exception)
        {
            System.err.println("File couldn't be created, aborting mod_ingameInfo!");
        }
        return null;
    }
    
    private int getX(int width)
    {
        if (alignMode == 1 || alignMode == 4 || alignMode == 7)
            return scaledResolution.getScaledWidth() / 2 - width / 2 + xOffset[alignMode];
        else if (alignMode == 2 || alignMode == 5 || alignMode == 8)
            return scaledResolution.getScaledWidth() - width - xOffset[alignMode];
        else
            return xOffset[alignMode];
    }
    
    private int getY(int rowCount, int rowNum)
    {
        if (alignMode == 3 || alignMode == 4 || alignMode == 5)
            return (scaledResolution.getScaledHeight() / 2) - (rowCount * 10 / 2) + (rowNum * 10) + yOffset[alignMode];
        else if (alignMode == 6 || alignMode == 7 || alignMode == 8)
            return scaledResolution.getScaledHeight() - (rowCount * 10) + (rowNum * 10) - yOffset[alignMode];
        else
            return (rowNum * 10) + yOffset[alignMode];
    }
    
    private String replaceAllTags(String s)
    {
        for (; s.indexOf('<') != -1; s = replaceTags(s))
        {}
        return s.replaceAll("&", "\247");
    }
    
    private String replaceTags(String s)
    {
        int startIndex = s.indexOf('<');
        int endIndex = s.indexOf('>');
        int openParamIndex = s.indexOf('[', startIndex);
        if (openParamIndex < endIndex)
            endIndex = s.indexOf('>', s.indexOf(']', startIndex));
        if (startIndex == -1)
            return s;
        else if (endIndex == -1)
            return s.replace("<", "");
        else
        {
            String s1 = s.substring(startIndex + 1, endIndex);
            s = s.replace((new StringBuilder()).append("<").append(s1).append(">").toString(), getTag(s1));
            return s;
        }
    }
    
    public String getTag(String s)
    {
        Coord coord = new Coord(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.posY), MathHelper.floor_double(mc.thePlayer.posZ));
        
        World world = mc.isIntegratedServerRunning() ? mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension) : mc.theWorld;
        BiomeGenBase biome = world.getBiomeGenForCoords(coord.x, coord.z);
        Chunk chunk = world.getChunkFromBlockCoords(coord.x, coord.z);
        
        /*
         * ********************************************************************************************************************
         * Utility tags
         */
        
        if (s.toLowerCase().startsWith("max"))
        {
            int startIndex = s.indexOf('[');
            int endIndex = s.indexOf('[', startIndex + 1);
            int divIndex = s.indexOf('/');
            if (s.indexOf('[') == -1 || endIndex == -1 || endIndex < divIndex)
                return "(ERROR: Incorrect syntax in MAX tag)";
            else
            {
                try
                {
                    float val1 = Float.valueOf(getTag(s.substring(startIndex + 1, divIndex).replace("<", "").replace(">", "")));
                    float val2 = Float.valueOf(getTag(s.substring(divIndex + 1, endIndex).replace("<", "").replace(">", "")));
                    return parseBoolean(s.substring(endIndex), val1 >= val2);
                }
                catch (NumberFormatException e)
                {
                    return "(MAX tag: NumberFormatException: '" + s.substring(startIndex + 1, divIndex) + "', '" + s.substring(divIndex + 1, endIndex) + "')";
                }
            }
        }
        if (s.toLowerCase().startsWith("pct"))
        {
            try
            {
                float value = Float.valueOf(getTag(s.substring(s.indexOf('(') + 1, s.indexOf(',')).replace("<", "").replace(">", "")));
                float pct = Float.valueOf(getTag(s.substring(s.indexOf(',') + 1, s.indexOf(')')).replace("<", "").replace(">", ""))) / 100F;
                return String.valueOf(MathHelper.floor_double(value * pct));
            }
            catch (NumberFormatException e)
            {
                return "(PCT tag: NumberFormatException: '" + s + "')";
            }
        }
        
        /*
         * ********************************************************************************************************************
         * Alignment tags
         */
        
        if (s.equalsIgnoreCase("left") || s.equalsIgnoreCase("topleft"))
        {
            alignMode = 0;
            return "";
        }
        if (s.equalsIgnoreCase("center") || s.equalsIgnoreCase("topcenter"))
        {
            alignMode = 1;
            return "";
        }
        if (s.equalsIgnoreCase("right") || s.equalsIgnoreCase("topright"))
        {
            alignMode = 2;
            return "";
        }
        if (s.equalsIgnoreCase("middleleft"))
        {
            alignMode = 3;
            return "";
        }
        if (s.equalsIgnoreCase("middlecenter"))
        {
            alignMode = 4;
            return "";
        }
        if (s.equalsIgnoreCase("middleright"))
        {
            alignMode = 5;
            return "";
        }
        if (s.equalsIgnoreCase("bottomleft"))
        {
            alignMode = 6;
            return "";
        }
        if (s.equalsIgnoreCase("bottomcenter"))
        {
            alignMode = 7;
            return "";
        }
        if (s.equalsIgnoreCase("bottomright"))
        {
            alignMode = 8;
            return "";
        }
        
        /*
         * ********************************************************************************************************************
         * Color tags
         */
        
        if (s.equalsIgnoreCase("black"))
        {
            return "\2470";
        }
        if (s.equalsIgnoreCase("darkblue") || s.equalsIgnoreCase("navy"))
        {
            return "\2471";
        }
        if (s.equalsIgnoreCase("darkgreen") || s.equalsIgnoreCase("green"))
        {
            return "\2472";
        }
        if (s.equalsIgnoreCase("darkaqua") || s.equalsIgnoreCase("darkcyan") || s.equalsIgnoreCase("turquoise"))
        {
            return "\2473";
        }
        if (s.equalsIgnoreCase("darkred"))
        {
            return "\2474";
        }
        if (s.equalsIgnoreCase("purple") || s.equalsIgnoreCase("violet"))
        {
            return "\2475";
        }
        if (s.equalsIgnoreCase("orange") || s.equalsIgnoreCase("gold"))
        {
            return "\2476";
        }
        if (s.equalsIgnoreCase("lightgrey") || s.equalsIgnoreCase("lightgray") || s.equalsIgnoreCase("grey") || s.equalsIgnoreCase("gray"))
        {
            return "\2477";
        }
        if (s.equalsIgnoreCase("darkgrey") || s.equalsIgnoreCase("darkgray") || s.equalsIgnoreCase("charcoal"))
        {
            return "\2478";
        }
        if (s.equalsIgnoreCase("indigo") || s.equalsIgnoreCase("blue") || s.equalsIgnoreCase("lightblue"))
        {
            return "\2479";
        }
        if (s.equalsIgnoreCase("brightgreen") || s.equalsIgnoreCase("lightgreen") || s.equalsIgnoreCase("lime"))
        {
            return "\247a";
        }
        if (s.equalsIgnoreCase("aqua") || s.equalsIgnoreCase("lightcyan") || s.equalsIgnoreCase("celeste") || s.equalsIgnoreCase("diamond"))
        {
            return "\247b";
        }
        if (s.equalsIgnoreCase("red") || s.equalsIgnoreCase("lightred") || s.equalsIgnoreCase("salmon"))
        {
            return "\247c";
        }
        if (s.equalsIgnoreCase("pink") || s.equalsIgnoreCase("magenta"))
        {
            return "\247d";
        }
        if (s.equalsIgnoreCase("yellow"))
        {
            return "\247e";
        }
        if (s.equalsIgnoreCase("white"))
        {
            return "\247f";
        }
        
        /*
         * ********************************************************************************************************************
         * Formatting tags
         */
        
        if (s.equalsIgnoreCase("random"))
        {
            return "\247k";
        }
        if (s.equalsIgnoreCase("bold") || s.equalsIgnoreCase("b"))
        {
            return "\247l";
        }
        if (s.equalsIgnoreCase("strikethrough") || s.equalsIgnoreCase("strike") || s.equalsIgnoreCase("s"))
        {
            return "\247m";
        }
        if (s.equalsIgnoreCase("underline") || s.equalsIgnoreCase("u"))
        {
            return "\247n";
        }
        if (s.equalsIgnoreCase("italic") || s.equalsIgnoreCase("italics") || s.equalsIgnoreCase("i"))
        {
            return "\247o";
        }
        if (s.equalsIgnoreCase("reset") || s.equalsIgnoreCase("r"))
        {
            return "\247r";
        }
        
        /*
         * ********************************************************************************************************************
         * World tags
         */
        
        if (s.toLowerCase().startsWith("irltime") || s.toLowerCase().startsWith("rltime"))
        {
            int startIndex = s.indexOf('[');
            int endIndex = s.indexOf(']', startIndex + 1);
            String fmt = "";
            
            if (startIndex != -1 && endIndex != -1)
                fmt = s.substring(startIndex + 1, endIndex);
            
            if (fmt.equals(""))
                fmt = "hh:mma";
            
            return new SimpleDateFormat(fmt).format(new Date());
        }
        if (s.toLowerCase().startsWith("mctime"))
        {
            int startIndex = s.indexOf('[');
            int endIndex = s.indexOf(']', startIndex + 1);
            long fmt = 12L;
            
            if (startIndex != -1 && endIndex != -1 && s.substring(startIndex + 1, endIndex).equals("24"))
                fmt = 24L;
            
            return CommonUtils.getMCTimeString(mc.theWorld.getWorldTime(), fmt);
        }
        if (s.equalsIgnoreCase("day"))
        {
            try
            {
                return Integer.toString((int) world.getWorldInfo().getWorldTime() / 24000);
            }
            catch (Throwable e)
            {
                return "0";
            }
        }
        if (s.equalsIgnoreCase("biome"))
        {
            try
            {
                return biome.biomeName;
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("dimension"))
        {
            // return mc.thePlayer.dimension == -1 ? "Nether" : mc.thePlayer.dimension == 1 ? "The End" : "Overworld";
            return mc.theWorld.provider.getDimensionName();
        }
        if (s.toLowerCase().startsWith("daytime"))
        {
            return parseBoolean(s, Boolean.valueOf(world.calculateSkylightSubtracted(1.0F) < 4));
        }
        if (s.toLowerCase().startsWith("raining"))
        {
            return parseBoolean(s, Boolean.valueOf(world.isRaining() && biome.canSpawnLightningBolt()));
        }
        if (s.toLowerCase().startsWith("snowing"))
        {
            return parseBoolean(s, Boolean.valueOf(world.isRaining() && !biome.canSpawnLightningBolt() && !biome.equals(BiomeGenBase.desert) && !biome.equals(BiomeGenBase.desertHills)));
        }
        if (s.equalsIgnoreCase("nextrain"))
        {
            return CommonUtils.ticksToTimeString(world.getWorldInfo().getRainTime());
        }
        if (s.toLowerCase().startsWith("thundering"))
        {
            return parseBoolean(s, Boolean.valueOf(world.getWorldInfo().isThundering() && biome.canSpawnLightningBolt()));
        }
        if (s.toLowerCase().startsWith("slimes"))
        {
            return parseBoolean(s, Boolean.valueOf(chunk.getRandomWithSeed(987234911L).nextInt(10) == 0));
        }
        if (s.toLowerCase().startsWith("hardcore"))
        {
            return parseBoolean(s, Boolean.valueOf(world.getWorldInfo().isHardcoreModeEnabled()));
        }
        if (s.equalsIgnoreCase("light"))
        {
            try
            {
                return Integer.toString(chunk.getBlockLightValue(coord.x & 0xf, coord.y, coord.z & 0xf, world.calculateSkylightSubtracted(1.0F)));
            }
            catch (Throwable e)
            {
                return "0";
            }
        }
        if (s.equalsIgnoreCase("lightfeet"))
        {
            try
            {
                return Integer.toString(chunk.getBlockLightValue(coord.x & 0xf, (int) Math.round(mc.thePlayer.boundingBox.minY), coord.z & 0xf, world.calculateSkylightSubtracted(1.0F)));
            }
            catch (Throwable e)
            {
                return "0";
            }
        }
        if (s.equalsIgnoreCase("lightnosun"))
        {
            try
            {
                return Integer.toString(chunk.getSavedLightValue(EnumSkyBlock.Block, coord.x & 0xf, coord.y, coord.z & 0xf));
            }
            catch (Throwable e)
            {
                return "0";
            }
        }
        if (s.equalsIgnoreCase("lightnosunfeet"))
        {
            try
            {
                return Integer.toString(chunk.getSavedLightValue(EnumSkyBlock.Block, coord.x & 0xf, (int) Math.round(mc.thePlayer.boundingBox.minY), coord.z & 0xf));
            }
            catch (Throwable e)
            {
                return "0";
            }
        }
        if (s.equalsIgnoreCase("x") || s.equalsIgnoreCase("xi"))
        {
            return Integer.toString(coord.x);
        }
        if (s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yi"))
        {
            return Integer.toString(coord.y);
        }
        if (s.equalsIgnoreCase("yfeet") || s.equalsIgnoreCase("yfeeti"))
        {
            return Integer.toString((int) Math.round(mc.thePlayer.boundingBox.minY));
        }
        if (s.equalsIgnoreCase("z") || s.equalsIgnoreCase("zi"))
        {
            return Integer.toString(coord.z);
        }
        if (s.equalsIgnoreCase("decx"))
        {
            return Double.toString(Math.round(mc.thePlayer.posX * 10.0) / 10.0);
        }
        if (s.equalsIgnoreCase("decy"))
        {
            return Double.toString(Math.round(mc.thePlayer.posY * 10.0) / 10.0);
        }
        if (s.equalsIgnoreCase("decyfeet"))
        {
            return Double.toString(Math.round(mc.thePlayer.boundingBox.minY * 10.0) / 10.0);
        }
        if (s.equalsIgnoreCase("decz"))
        {
            return Double.toString(Math.round(mc.thePlayer.posZ * 10.0) / 10.0);
        }
        if (s.equalsIgnoreCase("worldname"))
        {
            try
            {
                return world.getWorldInfo().getWorldName();
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("worldsize"))
        {
            try
            {
                return Long.toString(world.getWorldInfo().getSizeOnDisk());
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("worldsizemb"))
        {
            try
            {
                return Float.toString((((world.getWorldInfo().getSizeOnDisk() / 1024L) * 100L) / 1024L) / 100F);
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("seed"))
        {
            try
            {
                return mc.isIntegratedServerRunning() ? Long.toString(world.getSeed()) : "&onull";
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("roughdirection"))
        {
            try
            {
                switch (MathHelper.floor_double(((mc.thePlayer.rotationYaw * 4F) / 360F) + 0.5D) & 3)
                {
                    case 0:
                        return "South";
                    case 1:
                        return "West";
                    case 2:
                        return "North";
                    case 3:
                        return "East";
                }
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("finedirection"))
        {
            try
            {
                switch (MathHelper.floor_double(((mc.thePlayer.rotationYaw * 8F) / 360F) + 0.5D) & 7)
                {
                    case 0:
                        return "South";
                    case 1:
                        return "South West";
                    case 2:
                        return "West";
                    case 3:
                        return "North West";
                    case 4:
                        return "North";
                    case 5:
                        return "North East";
                    case 6:
                        return "East";
                    case 7:
                        return "South East";
                }
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("abrroughdirection"))
        {
            try
            {
                switch (MathHelper.floor_double(((mc.thePlayer.rotationYaw * 4F) / 360F) + 0.5D) & 3)
                {
                    case 0:
                        return "S";
                    case 1:
                        return "W";
                    case 2:
                        return "N";
                    case 3:
                        return "E";
                }
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("abrfinedirection"))
        {
            try
            {
                switch (MathHelper.floor_double(((mc.thePlayer.rotationYaw * 8F) / 360F) + 0.5D) & 7)
                {
                    case 0:
                        return "S";
                    case 1:
                        return "SW";
                    case 2:
                        return "W";
                    case 3:
                        return "NW";
                    case 4:
                        return "N";
                    case 5:
                        return "NE";
                    case 6:
                        return "E";
                    case 7:
                        return "SE";
                }
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("directionhud"))
        {
            try
            {
                int direction = MathHelper.floor_double(((mc.thePlayer.rotationYaw * 16F) / 360F) + 0.5D) & 15;
                if (direction == 0)
                    return "SE   &cS &f  SW";
                if (direction == 1)
                    return "  S    SW  ";
                if (direction == 2)
                    return "S    &cSW&f    W";
                if (direction == 3)
                    return "  SW    W  ";
                if (direction == 4)
                    return "SW   &cW &f  NW";
                if (direction == 5)
                    return "  W    NW  ";
                if (direction == 6)
                    return "W    &cNW&f    N";
                if (direction == 7)
                    return "  NW    N  ";
                if (direction == 8)
                    return "NW   &cN &f  NE";
                if (direction == 9)
                    return "  N    NE  ";
                if (direction == 10)
                    return "N    &cNE&f    E";
                if (direction == 11)
                    return "  NE    E  ";
                if (direction == 12)
                    return "NE   &cE &f  SE";
                if (direction == 13)
                    return "  E    SE  ";
                if (direction == 14)
                    return "E    &cSE&f    S";
                if (direction == 15)
                    return "  SE    S  ";
                else
                    return "this shit isn't working";
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        
        /*
         * ********************************************************************************************************************
         * Debug tags
         */
        
        if (s.equalsIgnoreCase("fps"))
        {
            try
            {
                return mc.debug.substring(0, mc.debug.indexOf(" fps"));
            }
            catch (Throwable e)
            {
                return "0";
            }
        }
        if (s.equalsIgnoreCase("entitiesrendered"))
        {
            try
            {
                String str = mc.getEntityDebug();
                return str.substring(str.indexOf(' ') + 1, str.indexOf('/'));
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("entitiestotal"))
        {
            try
            {
                String str = mc.getEntityDebug();
                return str.substring(str.indexOf('/') + 1, str.indexOf('.'));
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("memtotal"))
        {
            try
            {
                if (memoryUnit.equalsIgnoreCase("KB"))
                    return Long.toString(Runtime.getRuntime().totalMemory() / 1024L) + memoryUnit;
                if (memoryUnit.equalsIgnoreCase("MB"))
                    return Long.toString(Runtime.getRuntime().totalMemory() / 1024L / 1024L) + memoryUnit;
                if (memoryUnit.equalsIgnoreCase("GB"))
                    return Long.toString(Runtime.getRuntime().totalMemory() / 1024L / 1024L / 1024L) + memoryUnit;
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("memmax"))
        {
            try
            {
                if (memoryUnit.equalsIgnoreCase("KB"))
                    return Long.toString(Runtime.getRuntime().maxMemory() / 1024L) + memoryUnit;
                if (memoryUnit.equalsIgnoreCase("MB"))
                    return Long.toString(Runtime.getRuntime().maxMemory() / 1024L / 1024L) + memoryUnit;
                if (memoryUnit.equalsIgnoreCase("GB"))
                    return Long.toString(Runtime.getRuntime().maxMemory() / 1024L / 1024L / 1024L) + memoryUnit;
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("memfree"))
        {
            try
            {
                if (memoryUnit.equalsIgnoreCase("KB"))
                    return Long.toString(Runtime.getRuntime().freeMemory() / 1024L) + memoryUnit;
                if (memoryUnit.equalsIgnoreCase("MB"))
                    return Long.toString(Runtime.getRuntime().freeMemory() / 1024L / 1024L) + memoryUnit;
                if (memoryUnit.equalsIgnoreCase("GB"))
                    return Long.toString(Runtime.getRuntime().freeMemory() / 1024L / 1024L / 1024L) + memoryUnit;
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("memused"))
        {
            try
            {
                if (memoryUnit.equalsIgnoreCase("KB"))
                    return Long.toString((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L) + memoryUnit;
                if (memoryUnit.equalsIgnoreCase("MB"))
                    return Long.toString((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L / 1024L) + memoryUnit;
                if (memoryUnit.equalsIgnoreCase("GB"))
                    return Long.toString((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L / 1024L / 1024L) + memoryUnit;
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        
        /*
         * ********************************************************************************************************************
         * Player tags
         */
        if (s.equalsIgnoreCase("mouseover"))
        {
            MovingObjectPosition objectMouseOver = mc.objectMouseOver;
            if (objectMouseOver != null)
            {
                if (objectMouseOver.typeOfHit == EnumMovingObjectType.ENTITY)
                {
                    return objectMouseOver.entityHit.getEntityName();
                }
                else if (objectMouseOver.typeOfHit == EnumMovingObjectType.TILE)
                {
                    Block block = Block.blocksList[world.getBlockId(objectMouseOver.blockX, objectMouseOver.blockY, objectMouseOver.blockZ)];
                    if (block != null)
                    {
                        ItemStack pickBlock = block.getPickBlock(objectMouseOver, world, objectMouseOver.blockX, objectMouseOver.blockY, objectMouseOver.blockZ);
                        if (pickBlock != null)
                        {
                            return pickBlock.getDisplayName();
                        }
                        return block.getLocalizedName();
                    }
                }
            }
            return "";
        }
        if (s.equalsIgnoreCase("mouseoverid"))
        {
            MovingObjectPosition objectMouseOver = mc.objectMouseOver;
            if (objectMouseOver != null)
            {
                if (objectMouseOver.typeOfHit == EnumMovingObjectType.ENTITY)
                {
                    return objectMouseOver.entityHit.entityId + "";
                }
                else if (objectMouseOver.typeOfHit == EnumMovingObjectType.TILE)
                {
                    BlockID blockID = new BlockID(world, objectMouseOver.blockX, objectMouseOver.blockY, objectMouseOver.blockZ);
                    if (blockID.id != 0)
                    {
                        return blockID.toString();
                    }
                }
            }
            return "";
        }
        if (s.equalsIgnoreCase("mouseoverpowerweak"))
        {
            MovingObjectPosition objectMouseOver = mc.objectMouseOver;
            if (objectMouseOver != null)
            {
                if (objectMouseOver.typeOfHit == EnumMovingObjectType.TILE)
                {
                    Block block = Block.blocksList[world.getBlockId(objectMouseOver.blockX, objectMouseOver.blockY, objectMouseOver.blockZ)];
                    if (block != null)
                    {
                        int power = -1;
                        for (int side = 0; side < 6; side++)
                        {
                            power = Math.max(power, block.isProvidingWeakPower(world, objectMouseOver.blockX, objectMouseOver.blockY, objectMouseOver.blockZ, side));
                        }
                        return Integer.toString(power);
                    }
                }
            }
            return "-1";
        }
        if (s.equalsIgnoreCase("mouseoverpowerstrong"))
        {
            MovingObjectPosition objectMouseOver = mc.objectMouseOver;
            if (objectMouseOver != null)
            {
                if (objectMouseOver.typeOfHit == EnumMovingObjectType.TILE)
                {
                    Block block = Block.blocksList[world.getBlockId(objectMouseOver.blockX, objectMouseOver.blockY, objectMouseOver.blockZ)];
                    if (block != null)
                    {
                        int power = -1;
                        for (int side = 0; side < 6; side++)
                        {
                            power = Math.max(power, block.isProvidingStrongPower(world, objectMouseOver.blockX, objectMouseOver.blockY, objectMouseOver.blockZ, side));
                        }
                        return Integer.toString(power);
                    }
                }
            }
            return "-1";
        }
        if (s.equalsIgnoreCase("mouseoverpowerinput"))
        {
            MovingObjectPosition objectMouseOver = mc.objectMouseOver;
            if (objectMouseOver != null)
            {
                if (objectMouseOver.typeOfHit == EnumMovingObjectType.TILE)
                {
                    return Integer.toString(world.getBlockPowerInput(objectMouseOver.blockX, objectMouseOver.blockY, objectMouseOver.blockZ));
                }
            }
            return "-1";
        }
        if (s.equalsIgnoreCase("underwater") || s.equalsIgnoreCase("inwater"))
        {
            return Boolean.toString(mc.thePlayer.isInWater());
        }
        if (s.equalsIgnoreCase("wet"))
        {
            return Boolean.toString(mc.thePlayer.isWet());
        }
        if (s.equalsIgnoreCase("alive"))
        {
            return Boolean.toString(mc.thePlayer.isEntityAlive());
        }
        if (s.equalsIgnoreCase("burning"))
        {
            return Boolean.toString(mc.thePlayer.isBurning());
        }
        if (s.equalsIgnoreCase("riding"))
        {
            return Boolean.toString(mc.thePlayer.isRiding());
        }
        if (s.equalsIgnoreCase("sneaking"))
        {
            return Boolean.toString(mc.thePlayer.isSneaking());
        }
        if (s.equalsIgnoreCase("sprinting"))
        {
            return Boolean.toString(mc.thePlayer.isSprinting());
        }
        if (s.equalsIgnoreCase("invisible"))
        {
            return Boolean.toString(mc.thePlayer.isInvisible());
        }
        if (s.equalsIgnoreCase("eating"))
        {
            return Boolean.toString(mc.thePlayer.isEating());
        }
        if (s.equalsIgnoreCase("invulnerable"))
        {
            return Boolean.toString(mc.thePlayer.isEntityInvulnerable());
        }
        if (s.equalsIgnoreCase("gamemode"))
        {
            try
            {
                if (mc.thePlayer.capabilities.isCreativeMode)
                    return "Creative";
                else if (world.getWorldInfo().getGameType().equals(EnumGameType.SURVIVAL))
                    return "Survival";
                else if (world.getWorldInfo().getGameType().equals(EnumGameType.CREATIVE))
                    return "Creative/Survival?";
                else if (world.getWorldInfo().getGameType().equals(EnumGameType.ADVENTURE))
                    return "Adventure";
                else
                    return "???";
            }
            catch (Throwable e)
            {
                return "&onull";
            }
        }
        if (s.equalsIgnoreCase("score"))
        {
            try
            {
                return Integer.toString(mc.thePlayer.getScore());
            }
            catch (Throwable e)
            {
                return "0";
            }
        }
        if (s.equalsIgnoreCase("difficulty"))
        {
            if (mc.gameSettings.difficulty == 0)
                return "Peaceful";
            else if (mc.gameSettings.difficulty == 1)
                return "Easy";
            else if (mc.gameSettings.difficulty == 2)
                return "Normal";
            else if (mc.gameSettings.difficulty == 3)
                return "Hard";
        }
        if (s.equalsIgnoreCase("playerlevel"))
        {
            return Integer.toString(mc.thePlayer.experienceLevel);
        }
        if (s.equalsIgnoreCase("xpthislevel"))
        {
            return Integer.toString((int) Math.ceil(mc.thePlayer.experience * mc.thePlayer.xpBarCap()));
        }
        if (s.equalsIgnoreCase("xpuntilnext"))
        {
            return Integer.toString((int) ((1.0F - mc.thePlayer.experience) * mc.thePlayer.xpBarCap()));
        }
        if (s.equalsIgnoreCase("xpcap"))
        {
            return Integer.toString(mc.thePlayer.xpBarCap());
        }
        if (s.equalsIgnoreCase("username"))
        {
            return mc.thePlayer.username;
        }
        if (s.equalsIgnoreCase("texturepack"))
        {
            return mc.gameSettings.skin;
        }
        if (s.toLowerCase().startsWith("itemquantity"))
        {
            int startIndex = s.indexOf('[');
            int endIndex = s.indexOf(']', startIndex + 1);
            
            if (startIndex != -1 && endIndex != -1)
            {
                String ss = s.substring(startIndex + 1, endIndex).trim();
                String[] sa = ss.split(",");
                int id = CommonUtils.parseInt(sa[0].trim());
                int md = -1;
                if (sa.length > 1)
                    md = CommonUtils.parseInt(sa[1].trim());
                return HUDUtils.countInInventory(mc.thePlayer, id, md) + "";
            }
            
            return "itemquantity syntax error";
        }
        
        ItemStack itemStack = mc.thePlayer.getCurrentEquippedItem();
        if (s.equalsIgnoreCase("equippedquantity"))
        {
            if (itemStack != null)
            {
                Item item = itemStack.getItem();
                if (isForgeEnv)
                    return Integer.toString(HUDUtils.countInInventory(mc.thePlayer, itemStack.itemID, item.getDamage(itemStack)));
                else
                    return Integer.toString(HUDUtils.countInInventory(mc.thePlayer, itemStack.itemID, itemStack.getItemDamage()));
            }
            return "0";
        }
        
        if (s.matches("(equipped|helmet|chestplate|leggings|boots)(name|maxdamage|damage|damageleft)"))
        {
            if (s.startsWith("equipped"))
            {
                itemStack = mc.thePlayer.getCurrentEquippedItem();
            }
            else
            {
                int slot = -1;
                if (s.startsWith("helmet"))
                {
                    slot = 3;
                }
                else if (s.startsWith("chestplate"))
                {
                    slot = 2;
                }
                else if (s.startsWith("leggings"))
                {
                    slot = 1;
                }
                else if (s.startsWith("boots"))
                {
                    slot = 0;
                }
                itemStack = mc.thePlayer.inventory.armorItemInSlot(slot);
            }
            
            if (itemStack != null)
            {
                Item item = itemStack.getItem();
                
                if (item != null)
                {
                    if (s.endsWith("name"))
                    {
                        String arrows = itemStack.itemID == Item.bow.itemID ? " (" + HUDUtils.countInInventory(mc.thePlayer, Item.arrow.itemID, -1) + ")" : "";
                        return itemStack.getDisplayName() + arrows;
                    }
                    else if (s.endsWith("maxdamage"))
                    {
                        if (isForgeEnv)
                            return Integer.toString(item.isDamageable() ? item.getMaxDamage(itemStack) + 1 : 0);
                        else
                            return Integer.toString(itemStack.isItemStackDamageable() ? itemStack.getMaxDamage() + 1 : 0);
                    }
                    else if (s.endsWith("damage"))
                    {
                        if (isForgeEnv)
                            return Integer.toString(item.isDamageable() ? item.getDamage(itemStack) : 0);
                        else
                            return Integer.toString(itemStack.isItemStackDamageable() ? itemStack.getItemDamage() : 0);
                    }
                    else if (s.endsWith("damageleft"))
                    {
                        if (isForgeEnv)
                            return Integer.toString(item.isDamageable() ? item.getMaxDamage(itemStack) + 1 - item.getDamage(itemStack) : 0);
                        else
                            return Integer.toString(itemStack.isItemStackDamageable() ? (itemStack.getMaxDamage() + 1) - (itemStack.getItemDamage()) : 0);
                    }
                }
            }
        }
        
        try
        {
            Float.valueOf(s);
            return s;
        }
        catch (Throwable e)
        {
            return "";
        }
    }
    
    private String parseBoolean(String s, Boolean boolean1)
    {
        String s1 = "";
        if (s.indexOf('[') == -1 || s.indexOf('/') == -1 || s.indexOf(']') == -1)
        {
            return s1;
        }
        if (boolean1.booleanValue())
        {
            s1 = s.substring(s.indexOf('[') + 1, s.indexOf('/'));
        }
        else
        {
            s1 = s.substring(s.indexOf('/') + 1, s.indexOf(']'));
        }
        return s1;
    }
    
    public class CommandIGI extends CommandBase
    {
        @Override
        public String getCommandName()
        {
            return "igi";
        }
        
        @Override
        public String getCommandUsage(ICommandSender icommandsender)
        {
            return "commands.igi.usage";
        }
        
        @Override
        public void processCommand(ICommandSender icommandsender, String[] args)
        {
            if (args.length == 1)
            {
                if (args[0].equalsIgnoreCase("reload"))
                {
                    loadFormatFile();
                    enabled = true;
                    return;
                }
                else if (args[0].equalsIgnoreCase("enable"))
                {
                    enabled = true;
                    return;
                }
                else if (args[0].equalsIgnoreCase("disable"))
                {
                    enabled = false;
                    return;
                }
                else if (args[0].equalsIgnoreCase("toggle"))
                {
                    enabled = !enabled;
                    return;
                }
            }
            
            throw new WrongUsageException("commands.igi.usage", new Object[0]);
        }
    }
}
