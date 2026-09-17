package com.moneybags.tempfly.aesthetic;

import org.bukkit.entity.Player;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.time.TimeManager.Placeholder;
import com.moneybags.tempfly.util.V;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class ClipAPI {
	
	private static TempFly tempfly;
	private static PlaceHolders instance;
	
	public static void initialize(TempFly plugin) {
		tempfly = plugin;
		if (instance == null) {
			instance = new PlaceHolders();
			instance.register();
		}
	}
	
	public static class PlaceHolders extends PlaceholderExpansion {
		
		@Override
		public boolean persist() {
			return true;
		}
		
	    @Override
	    public boolean canRegister(){
	        return true;
	    }
		
		@Override
		public String getAuthor() {
			return "ChiefMoneyBags";
		}

		@Override
		public String getIdentifier() {
			return "tempfly";
		}

		@Override
		public String getVersion() {
			return tempfly.getDescription().getVersion();
		}
		
		@Override
		public String onPlaceholderRequest(Player p, String identifier) {
			if (p == null) {
				return null;
			}
			switch (identifier.toLowerCase()) {
			case "time-formatted":
			case "time_formatted":
				return tempfly.getTimeManager().getPlaceHolder(p, Placeholder.TIME_FORMATTED);
			case "time-days":
			case "time_days":
				return tempfly.getTimeManager().getPlaceHolder(p, Placeholder.TIME_DAYS);
			case "time-hours":
			case "time_hours":
				return tempfly.getTimeManager().getPlaceHolder(p, Placeholder.TIME_HOURS);
			case "time-minutes":
			case "time_minutes":
				return tempfly.getTimeManager().getPlaceHolder(p, Placeholder.TIME_MINUTES);
			case "time-seconds":
			case "time_seconds":
				return tempfly.getTimeManager().getPlaceHolder(p, Placeholder.TIME_SECONDS);
			case "time-seconds-total":
			case "time_seconds_total":
				return tempfly.getTimeManager().getPlaceHolder(p, Placeholder.TIME_SECONDS_TOTAL);
			case "list-name":
			case "list_name":
				return p.isOnline() ? tempfly.getFlightManager().getUser(p).getListPlaceholder() : "";
			case "name-tag":
			case "name_tag":
				return p.isOnline() ? tempfly.getFlightManager().getUser(p).getTagPlaceholder() : "";
			case "infinite":
			case "is-infinite":
			case "is_infinite":
			{
				com.moneybags.tempfly.user.FlightUser u = tempfly.getFlightManager().getUser(p);
				String yes = V.placeholderInfiniteYes != null ? V.placeholderInfiniteYes : (V.infinity != null ? V.infinity : "∞");
				String no = V.placeholderInfiniteNo != null ? V.placeholderInfiniteNo : "";
				return u != null && u.hasInfiniteFlight() ? yes : no;
			}
			case "flying":
			case "is-flying":
			case "is_flying":
			{
				com.moneybags.tempfly.user.FlightUser u = tempfly.getFlightManager().getUser(p);
				String yes = V.placeholderFlyingYes != null ? V.placeholderFlyingYes : "true";
				String no = V.placeholderFlyingNo != null ? V.placeholderFlyingNo : "false";
				return u != null && u.hasFlightEnabled() ? yes : no;
			}
			case "speed":
			{
				return p.isOnline() ? new java.text.DecimalFormat("#.##").format(p.getFlySpeed() * 10) : "1";
			}
			default:
				return V.placeholderInvalid != null && !V.placeholderInvalid.isEmpty() ? V.placeholderInvalid : null;
			}
		}
	}
}
