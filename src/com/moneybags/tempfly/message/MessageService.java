package com.moneybags.tempfly.message;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * High-performance MessageService with:
 * 1. Bounded/Thread-safe MiniMessage template compilation cache to eliminate build lag.
 * 2. Strict MiniMessage tag-injection prevention using Placeholder.unparsed.
 * 3. Transparent backward-compatibility for legacy color codes (&a, &c, §).
 */
public class MessageService {

	private static final int MAX_CACHE_ENTRIES = 1024;
	private static final Pattern BRACKET_PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Za-z0-9_]+)}");

	private final MiniMessage miniMessage;
	private final LegacyComponentSerializer legacySerializer;
	private final Map<String, String> templateCache;
	private final Map<String, Component> staticComponentCache;

	public MessageService() {
		this.miniMessage = MiniMessage.miniMessage();
		this.legacySerializer = LegacyComponentSerializer.legacySection();
		this.templateCache = new ConcurrentHashMap<>();
		this.staticComponentCache = new ConcurrentHashMap<>();
	}

	/**
	 * Clears the template and component caches (e.g. on config reload).
	 */
	public void clearCache() {
		templateCache.clear();
		staticComponentCache.clear();
	}

	/**
	 * Pre-processes a raw template string:
	 * - Normalizes legacy &amp; color codes into MiniMessage tags.
	 * - Normalizes {PLACEHOLDER} bracket syntax into &lt;placeholder&gt; syntax.
	 * Results are cached to ensure templates are parsed only once.
	 */
	public String normalizeTemplate(@Nullable String raw) {
		if (raw == null || raw.isEmpty()) {
			return "";
		}
		if (templateCache.size() > MAX_CACHE_ENTRIES) {
			templateCache.clear();
		}
		return templateCache.computeIfAbsent(raw, this::convertRawToMiniMessageTemplate);
	}

	private String convertRawToMiniMessageTemplate(String raw) {
		String result = raw;

		// Convert {PLACEHOLDER} into <placeholder>
		Matcher bracketMatcher = BRACKET_PLACEHOLDER_PATTERN.matcher(result);
		StringBuilder sb = new StringBuilder();
		while (bracketMatcher.find()) {
			String key = bracketMatcher.group(1).toLowerCase(Locale.ROOT);
			bracketMatcher.appendReplacement(sb, "<" + key + ">");
		}
		bracketMatcher.appendTail(sb);
		result = sb.toString();

		// Convert legacy & or § color codes if present
		if (result.indexOf('&') != -1 || result.indexOf('§') != -1) {
			result = convertLegacyCodes(result);
		}

		return result;
	}

	private static final Pattern HEX_AMPERSAND_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
	private static final Pattern BUKKIT_HEX_PATTERN = Pattern.compile("§x(§[0-9a-fA-F]){6}");
	private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("[&§]([0-9a-fk-orA-FK-OR])");

	private String convertLegacyCodes(String input) {
		String result = input;

		// Convert &#RRGGBB hex format to <#RRGGBB>
		if (result.contains("&#")) {
			Matcher hexMatcher = HEX_AMPERSAND_PATTERN.matcher(result);
			result = hexMatcher.replaceAll("<#$1>");
		}

		// Convert Bukkit §x§r§r§g§g§b§b hex format to <#RRGGBB>
		if (result.contains("§x") || result.contains("§X")) {
			Matcher bukkitHexMatcher = BUKKIT_HEX_PATTERN.matcher(result);
			StringBuilder sb = new StringBuilder();
			while (bukkitHexMatcher.find()) {
				String matched = bukkitHexMatcher.group(0);
				// Extract characters at indices 3, 5, 7, 9, 11, 13
				StringBuilder hex = new StringBuilder("<#");
				for (int i = 3; i < matched.length(); i += 2) {
					hex.append(matched.charAt(i));
				}
				hex.append('>');
				bukkitHexMatcher.appendReplacement(sb, Matcher.quoteReplacement(hex.toString()));
			}
			bukkitHexMatcher.appendTail(sb);
			result = sb.toString();
		}

		// Convert single code legacy format [&§][0-9a-fk-or]
		Matcher matcher = LEGACY_CODE_PATTERN.matcher(result);
		StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			char code = Character.toLowerCase(matcher.group(1).charAt(0));
			String replacement = switch (code) {
				case '0' -> "<black>";
				case '1' -> "<dark_blue>";
				case '2' -> "<dark_green>";
				case '3' -> "<dark_aqua>";
				case '4' -> "<dark_red>";
				case '5' -> "<dark_purple>";
				case '6' -> "<gold>";
				case '7' -> "<gray>";
				case '8' -> "<dark_gray>";
				case '9' -> "<blue>";
				case 'a' -> "<green>";
				case 'b' -> "<aqua>";
				case 'c' -> "<red>";
				case 'd' -> "<light_purple>";
				case 'e' -> "<yellow>";
				case 'f' -> "<white>";
				case 'k' -> "<obfuscated>";
				case 'l' -> "<bold>";
				case 'm' -> "<strikethrough>";
				case 'n' -> "<underlined>";
				case 'o' -> "<italic>";
				case 'r' -> "<reset>";
				default -> matcher.group(0);
			};
			matcher.appendReplacement(sb, replacement);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Parses a static template without dynamic arguments, using the component cache.
	 */
	public Component parse(@Nullable String raw) {
		if (raw == null || raw.isEmpty()) {
			return Component.empty();
		}
		if (staticComponentCache.size() > MAX_CACHE_ENTRIES) {
			staticComponentCache.clear();
		}
		return staticComponentCache.computeIfAbsent(raw, str -> {
			String normalized = normalizeTemplate(str);
			return miniMessage.deserialize(normalized);
		});
	}

	/**
	 * Parses a dynamic template with dynamic string placeholders.
	 * All string values are wrapped via Placeholder.unparsed, guaranteeing
	 * that user inputs cannot inject arbitrary MiniMessage tags.
	 */
	public Component parse(@Nullable String raw, @NotNull Map<String, String> placeholders) {
		if (raw == null || raw.isEmpty()) {
			return Component.empty();
		}
		if (placeholders.isEmpty()) {
			return parse(raw);
		}

		String normalized = normalizeTemplate(raw);
		TagResolver[] resolvers = new TagResolver[placeholders.size()];
		int i = 0;
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			String key = entry.getKey().toLowerCase(Locale.ROOT);
			String val = entry.getValue() != null ? entry.getValue() : "";
			resolvers[i++] = Placeholder.unparsed(key, val);
		}

		return miniMessage.deserialize(normalized, TagResolver.resolver(resolvers));
	}

	/**
	 * Parses a template with custom TagResolvers.
	 */
	public Component parse(@Nullable String raw, TagResolver... resolvers) {
		if (raw == null || raw.isEmpty()) {
			return Component.empty();
		}
		if (resolvers == null || resolvers.length == 0) {
			return parse(raw);
		}
		String normalized = normalizeTemplate(raw);
		return miniMessage.deserialize(normalized, resolvers);
	}

	/**
	 * Sends a parsed message to the specified Audience.
	 */
	public void send(@NotNull Audience audience, @Nullable String raw) {
		if (audience == null || raw == null || raw.isEmpty()) return;
		audience.sendMessage(parse(raw));
	}

	/**
	 * Sends a parsed message with sanitized placeholders to the specified Audience.
	 */
	public void send(@NotNull Audience audience, @Nullable String raw, @NotNull Map<String, String> placeholders) {
		if (audience == null || raw == null || raw.isEmpty()) return;
		audience.sendMessage(parse(raw, placeholders));
	}

	/**
	 * Sends an action bar to the specified Audience with sanitized placeholders.
	 */
	public void sendActionBar(@NotNull Audience audience, @Nullable String raw, @NotNull Map<String, String> placeholders) {
		if (audience == null || raw == null || raw.isEmpty()) return;
		audience.sendActionBar(parse(raw, placeholders));
	}

	/**
	 * Serializes a Component to legacy section string format (&sect;) for backward-compatible consumers.
	 */
	public String toLegacy(@NotNull Component component) {
		return legacySerializer.serialize(component);
	}
}
