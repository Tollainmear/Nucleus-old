/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.WarpLocation;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Permissions(root = "home", alias = "list", suggestedLevel = SuggestedLevel.USER)
@RunAsync
@NoCooldown
@NoWarmup
@NoCost
@RegisterCommand({"listhomes", "homes"})
public class ListHomeCommand extends CommandBase<CommandSource> {

    private final String player = "player";

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", new PermissionInformation(Util.getMessageWithFormat("permission.others"), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.optional(GenericArguments.onlyOne(
                GenericArguments.requiringPermission(GenericArguments.user(Text.of(player)), permissions.getPermissionWithSuffix("others"))))};
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Optional<User> ou = this.getUser(User.class, src, player, args); // args.getOne(player);
        if (!ou.isPresent()) {
            return CommandResult.empty();
        }

        Text header;
        User user = ou.get();

        boolean other = !src.equals(user);

        Map<String, WarpLocation> msw = plugin.getUserLoader().getUser(user).getHomes();
        if (msw.isEmpty()) {
            src.sendMessage(Util.getTextMessageWithFormat("command.home.nohomes"));
            return CommandResult.empty();
        }

        if (other) {
            header = Util.getTextMessageWithFormat("home.title.name", user.getName());
        } else {
            header = Util.getTextMessageWithFormat("home.title.normal");
        }

        List<Text> lt = msw.entrySet().stream().sorted((x, y) -> x.getKey().compareTo(y.getKey())).map(x -> {
            Location<World> lw = x.getValue().getLocation();
            return Text
                    .builder().append(
                            Text.builder(x.getKey()).color(TextColors.GREEN).style(TextStyles.UNDERLINE)
                                    .onHover(TextActions.showText(Util.getTextMessageWithFormat("home.warphover", x.getKey())))
                                    .onClick(TextActions.runCommand(other ? "/homeother " + user.getName() + " " + x.getValue().getName()
                                            : "/home " + x.getValue().getName()))
                                    .build())
                    .append(Util.getTextMessageWithFormat("home.location", lw.getExtent().getName(), String.valueOf(lw.getBlockX()),
                            String.valueOf(lw.getBlockY()), String.valueOf(lw.getBlockZ())))
                    .build();
        }).collect(Collectors.toList());

        PaginationService ps = Sponge.getServiceManager().provideUnchecked(PaginationService.class);
        ps.builder().title(Text.of(TextColors.YELLOW, header)).padding(Text.of(TextColors.GREEN, "-")).contents(lt).sendTo(src);
        return CommandResult.success();
    }
}
