package com.moneybags.tempfly.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class MessageServiceTest {

	private MessageService messageService;

	@BeforeEach
	public void setUp() {
		messageService = new MessageService();
	}

	@Test
	public void testStaticTemplateCaching() {
		String template = "<green>Your flight has been enabled!";
		Component first = messageService.parse(template);
		Component second = messageService.parse(template);

		assertNotNull(first);
		assertSame(first, second, "Static components must be cached and return identical instance");
	}

	@Test
	public void testLegacyAmpersandColorCodeConversion() {
		String legacy = "&aFlight &cDisabled";
		Component comp = messageService.parse(legacy);

		String plain = PlainTextComponentSerializer.plainText().serialize(comp);
		assertEquals("Flight Disabled", plain);
	}

	@Test
	public void testSectionColorCodeConversion() {
		String legacy = "§aFlight §cDisabled";
		Component comp = messageService.parse(legacy);

		String plain = PlainTextComponentSerializer.plainText().serialize(comp);
		assertEquals("Flight Disabled", plain);
	}

	@Test
	public void testHexColorConversion() {
		String hexAmp = "&#FFAA00Gold Text";
		Component compAmp = messageService.parse(hexAmp);
		String plainAmp = PlainTextComponentSerializer.plainText().serialize(compAmp);
		assertEquals("Gold Text", plainAmp);

		String bukkitHex = "§x§f§f§a§a§0§0Gold Text";
		Component compBukkit = messageService.parse(bukkitHex);
		String plainBukkit = PlainTextComponentSerializer.plainText().serialize(compBukkit);
		assertEquals("Gold Text", plainBukkit);
	}

	@Test
	public void testBracketPlaceholderNormalization() {
		String template = "Player: {PLAYER}, Time: {TIME}";
		Component comp = messageService.parse(template, Map.of(
				"player", "Notch",
				"time", "10m"
		));

		String plain = PlainTextComponentSerializer.plainText().serialize(comp);
		assertEquals("Player: Notch, Time: 10m", plain);
	}

	@Test
	public void testTagInjectionPrevention() {
		// An attacker attempts to inject a click event tag via their player name
		String maliciousInput = "<click:run_command:/op evil>EvilPlayer</click>";
		String template = "Welcome {PLAYER} to the server!";

		Component comp = messageService.parse(template, Map.of("player", maliciousInput));

		// Plain text must contain the literal angle brackets
		String plain = PlainTextComponentSerializer.plainText().serialize(comp);
		assertTrue(plain.contains("<click:run_command:/op evil>EvilPlayer</click>"), 
				"Malicious tags must not be evaluated, they must remain literal text");

		// Inspect all component children to ensure NO ClickEvent exists
		assertNull(comp.clickEvent(), "Root component must not have a ClickEvent");
		for (Component child : comp.children()) {
			assertNull(child.clickEvent(), "Child component must not have a ClickEvent");
			assertNull(child.hoverEvent(), "Child component must not have a HoverEvent");
		}
	}

	@Test
	public void testNullAndEmptyHandling() {
		assertEquals(Component.empty(), messageService.parse(null));
		assertEquals(Component.empty(), messageService.parse(""));
		assertEquals("", messageService.normalizeTemplate(null));
	}
}
