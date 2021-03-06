/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister & Julian König
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.devcordde.devcordbot.command.context

import com.github.devcordde.devcordbot.command.AbstractCommand
import com.github.devcordde.devcordbot.command.CommandClient
import com.github.devcordde.devcordbot.command.impl.CommandClientImpl
import com.github.devcordde.devcordbot.command.permission.Permission
import com.github.devcordde.devcordbot.command.permission.PermissionState
import com.github.devcordde.devcordbot.constants.Embeds
import com.github.devcordde.devcordbot.core.DevCordBot
import com.github.devcordde.devcordbot.database.DevCordUser
import com.github.devcordde.devcordbot.dsl.EmbedConvention
import com.github.devcordde.devcordbot.dsl.sendMessage
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.MessageAction

/**
 * Representation of a context of a command execution.
 * @property command the executed command
 * @property args the [Arguments] of the command
 * @property commandClient the [CommandClient] which executed this command
 * @property bot instance of the [DevCordBot]
 * @property message the message that triggered the command
 * @property devCordUser User storing database settings. See [DevCordUser]
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
data class Context(
    val bot: DevCordBot,
    val command: AbstractCommand,
    val args: Arguments,
    val message: Message,
    val commandClient: CommandClient,
    val devCordUser: DevCordUser
) {
    /**
     * The [JDA] instance.
     */
    val jda: JDA
        get() = message.jda

    /**
     * The id of [message].
     */
    val messageId: Long
        get() = message.idLong

    /**
     * The [TextChannel] of [message].
     */
    val channel: MessageChannel
        get() = message.channel

    /**
     * The author of the [message].
     */
    val author: User
        get() = message.author

    /**
     * The member of the [author].
     */
    val member: Member?
        get() = if (message.isFromType(ChannelType.TEXT)) message.member else bot.guild.getMemberById(author.id)

    /**
     * The guild of the [channel].
     */
    val guild: Guild
        get() = if (message.channelType == ChannelType.TEXT) message.guild else bot.guild

    /**
     * The [self member][Member] of the bot.
     */
    val me: Member
        get() = guild.selfMember

    /**
     * The [SelfUser] of the bot.
     */
    val selfUser: SelfUser
        get() = jda.selfUser

    /**
     * Sends [content] into [channel].
     * @return a [MessageAction] that sends the message
     */
    fun respond(content: String): RestAction<Message> {
        val message =
            MessageBuilder(content).denyMentions(Message.MentionType.EVERYONE, Message.MentionType.HERE).build()
        return notifyCommandHandler(channel.sendMessage(message))
    }

    /**
     * Sends [embed] into [channel].
     * @return a [MessageAction] that sends the message
     */
    fun respond(embed: MessageEmbed): RestAction<Message> = notifyCommandHandler(channel.sendMessage(embed))

    /**
     * Sends [embedBuilder] into [channel].
     * @return a [MessageAction] that sends the message
     */
    fun respond(embedBuilder: EmbedBuilder): RestAction<Message> =
        notifyCommandHandler(channel.sendMessage(embedBuilder.build()))

    /**
     * Sends [embed] into [channel].
     * @return a [MessageAction] that sends the message
     */
    fun respond(embed: EmbedConvention): RestAction<Message> = notifyCommandHandler(channel.sendMessage(embed))

    /**
     * Sends a help embed for [command].
     * @see Embeds.command
     */
    fun sendHelp(): RestAction<Message> = respond(Embeds.command(command))

    /**
     * Checks whether the [member] has [Permission.ADMIN] or not.
     */
    fun hasAdmin(): Boolean = hasPermission(Permission.ADMIN)

    /**
     * Checks whether the [member] has [Permission.MODERATOR] or not.
     */
    fun hasModerator(): Boolean = hasPermission(Permission.MODERATOR)

    /**
     * Checks if [devCordUser] has [permission].
     */
    fun hasPermission(permission: Permission): Boolean =
        commandClient.permissionHandler.isCovered(permission, member, devCordUser) == PermissionState.ACCEPTED

    private fun notifyCommandHandler(action: MessageAction) = action.map {
        (commandClient as CommandClientImpl).acknowledgeResponse(message.idLong, it.channel.idLong, it.idLong); it
    }

}
