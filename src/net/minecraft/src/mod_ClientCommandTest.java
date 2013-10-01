package net.minecraft.src;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraftforge.client.ClientCommandHandler;

public class mod_ClientCommandTest extends BaseMod
{
    @Override
    public String getName()
    {
        return "ClientCommandTest";
    }
    
    @Override
    public String getVersion()
    {
        return "1.0";
    }
    
    @Override
    public void load()
    {
        ModLoader.addLocalization("commands.igi.usage", "igi [reload/enable/disable/toggle]");
        
        // This line registers the command on the client side. When using "/igi reload" this command fails
        ClientCommandHandler.instance.registerCommand(new CommandIGI());
        
        // This line registers the command with the integrated server. When using "/igi reload" this command succeeds since the client version failed
        ModLoader.addCommand(new CommandIGI());
        
        /*
         * Expected output from executing "/igi reload":
         *      igi reload command processed. <- from the client command
         * Actual output:
         *      You do not have permission to use this command <- from the client command
         *      igi reload command processed. <- from the integrated server command
         */
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
                    Minecraft.getMinecraft().thePlayer.addChatMessage("igi reload command processed.");
                    return;
                }
                else if (args[0].equalsIgnoreCase("enable"))
                {
                    Minecraft.getMinecraft().thePlayer.addChatMessage("igi enable command processed.");
                    return;
                }
                else if (args[0].equalsIgnoreCase("disable"))
                {
                    Minecraft.getMinecraft().thePlayer.addChatMessage("igi disable command processed.");
                    return;
                }
                else if (args[0].equalsIgnoreCase("toggle"))
                {
                    Minecraft.getMinecraft().thePlayer.addChatMessage("igi toggle command processed.");
                    return;
                }
            }
            
            throw new WrongUsageException("commands.igi.usage", new Object[0]);
        }
    }
}
