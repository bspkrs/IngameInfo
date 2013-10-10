package bspkrs.ingameinfo;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import cpw.mods.fml.common.registry.LanguageRegistry;

public class CommandIGI extends CommandBase
{
    public CommandIGI()
    {
        super();
        LanguageRegistry.instance().addStringLocalization("commands.igi.usage", "igi [reload/enable/disable/toggle]");
    }
    
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
    public boolean canCommandSenderUseCommand(ICommandSender par1iCommandSender)
    {
        return true;
    }
    
    @Override
    public void processCommand(ICommandSender icommandsender, String[] args)
    {
        if (args.length == 1)
        {
            if (args[0].equalsIgnoreCase("reload"))
            {
                IngameInfo.loadFormatFile();
                IngameInfo.enabled = true;
                return;
            }
            else if (args[0].equalsIgnoreCase("enable"))
            {
                IngameInfo.enabled = true;
                return;
            }
            else if (args[0].equalsIgnoreCase("disable"))
            {
                IngameInfo.enabled = false;
                return;
            }
            else if (args[0].equalsIgnoreCase("toggle"))
            {
                IngameInfo.enabled = !IngameInfo.enabled;
                return;
            }
        }
        
        throw new WrongUsageException("commands.igi.usage", new Object[0]);
    }
}
