package org.mtransit.parser.ca_squamish_transit_system_bus;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.commons.StrategicMappingCommons;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

// https://bctransit.com/*/footer/open-data
// https://bctransit.com/servlet/bctransit/data/GTFS - Squamish
// https://squamish.mapstrat.com/current/google_transit.zip
public class SquamishTransitSystemBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-squamish-transit-system-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new SquamishTransitSystemBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		MTLog.log("Generating Squamish Transit System bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating Squamish Transit System bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	private static final String INCLUDE_ONLY_SERVICE_ID_CONTAINS = null;

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		//noinspection ConstantConditions
		if (INCLUDE_ONLY_SERVICE_ID_CONTAINS != null
				&& !gCalendar.getServiceId().contains(INCLUDE_ONLY_SERVICE_ID_CONTAINS)) {
			return true;
		}
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		//noinspection ConstantConditions
		if (INCLUDE_ONLY_SERVICE_ID_CONTAINS != null
				&& !gCalendarDates.getServiceId().contains(INCLUDE_ONLY_SERVICE_ID_CONTAINS)) {
			return true;
		}
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	private static final String INCLUDE_AGENCY_ID = "1"; // Squamish Transit System only

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!INCLUDE_AGENCY_ID.equals(gRoute.getAgencyId())) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		//noinspection ConstantConditions
		if (INCLUDE_ONLY_SERVICE_ID_CONTAINS != null
				&& !gTrip.getServiceId().contains(INCLUDE_ONLY_SERVICE_ID_CONTAINS)) {
			return true;
		}
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) { // used by GTFS-RT
		return super.getRouteId(gRoute);
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = CleanUtils.cleanSlashes(routeLongName);
		routeLongName = CleanUtils.cleanNumbers(routeLongName);
		routeLongName = CleanUtils.cleanStreetTypes(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_GREEN = "34B233";// GREEN (from PDF Corporate Graphic Standards)
	@SuppressWarnings("unused")
	private static final String AGENCY_COLOR_BLUE = "002C77"; // BLUE (from PDF Corporate Graphic Standards)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 1: return "004B8D";
			case 2: return "8CC63F";
			case 3: return "F78B1F";
			case 4: return "FB298C";
			case 5: return "00ADEE";
			case 9: return "AB5C3B";
			// @formatter:on
			default:
				MTLog.logFatal("Unexpected route color for %s!", gRoute);
				return null;
			}
		}
		return super.getRouteColor(gRoute);
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		map2.put(3L, new RouteTripSpec(3L, // 3 //
				StrategicMappingCommons.COUNTERCLOCKWISE_0, MTrip.HEADSIGN_TYPE_STRING, "Downtown", //
				StrategicMappingCommons.COUNTERCLOCKWISE_1, MTrip.HEADSIGN_TYPE_STRING, "Valleycliffe") //
				.addTripSort(StrategicMappingCommons.COUNTERCLOCKWISE_0, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("102762"), // Northbound Spruce at Chestnut
								Stops.getALL_STOPS().get("102749"), // ++ Westway at Cedar (SB)
								Stops.getALL_STOPS().get("102729"), // Westbound Pemberton at Third #DOWNTOWN
								Stops.getALL_STOPS().get("102738") // 2nd @ Main (SB) #DOWNTOWN
						)) //
				.addTripSort(StrategicMappingCommons.COUNTERCLOCKWISE_1, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("102738"), // 2nd @ Main (SB) #DOWNTOWN
								Stops.getALL_STOPS().get("102734"), // == Cleveland at Hunter (EB)
								Stops.getALL_STOPS().get("102706"), // != Behrner at Clarke (NB)
								Stops.getALL_STOPS().get("102705"), // != Clarke at Behrner (SB)
								Stops.getALL_STOPS().get("102747"), // == Guilford at Westway (EB)
								Stops.getALL_STOPS().get("102762") // Northbound Spruce at Chestnut
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private final HashMap<Long, Long> routeIdToShortName = new HashMap<>();

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		final long rsn = Long.parseLong(mRoute.getShortName());
		this.routeIdToShortName.put(mRoute.getId(), rsn);
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		final long rsn = this.routeIdToShortName.get(mTrip.getRouteId());
		if (rsn == 1L) {
			if (Arrays.asList( //
					"Garibaldi Vlg", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (rsn == 2L) {
			if (Arrays.asList( //
					"Garibaldi Vlg", //
					"Brackendale", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (rsn == 3L) {
			if (Arrays.asList( //
					"Valleycliffe-Spruce Loop Only", //
					"Valleycliffe" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Valleycliffe", mTrip.getHeadsignId());
				return true;
			}
		}
		MTLog.logFatal("Unexpected trips to merge: %s & %s!", mTrip, mTripToMerge);
		return false;
	}

	private static final String EXCH = "Exch";
	private static final Pattern EXCHANGE = Pattern.compile("((^|\\W)(exchange)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = "$2" + EXCH + "$4";

	private static final Pattern ENDS_WITH_DASH_VIA = Pattern.compile("(-via .*$)", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_DASH_TO = Pattern.compile("(^.*-to )", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = EXCHANGE.matcher(tripHeadsign).replaceAll(EXCHANGE_REPLACEMENT);
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = ENDS_WITH_DASH_VIA.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = STARTS_WITH_DASH_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern STARTS_WITH_IMPL = Pattern.compile("(^(\\(-IMPL-\\)))", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = STARTS_WITH_IMPL.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = EXCHANGE.matcher(gStopName).replaceAll(EXCHANGE_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) { // used by GTFS-RT
		return super.getStopId(gStop);
	}
}
