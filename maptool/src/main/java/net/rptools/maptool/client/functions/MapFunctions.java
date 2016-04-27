/*
 * This software copyright by various authors including the RPTools.net
 * development team, and licensed under the LGPL Version 3 or, at your option,
 * any later version.
 *
 * Portions of this software were originally covered under the Apache Software
 * License, Version 1.1 or Version 2.0.
 *
 * See the file LICENSE elsewhere in this distribution for license details.
 */

package net.rptools.maptool.client.functions;

import java.util.LinkedList;
import java.util.List;

import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.ui.zone.ZoneRenderer;
import net.rptools.maptool.language.I18N;
import net.rptools.maptool.model.Zone;
import net.rptools.parser.Parser;
import net.rptools.parser.ParserException;
import net.rptools.parser.function.AbstractFunction;
import net.sf.json.JSONArray;

public class MapFunctions extends AbstractFunction {
	private static final MapFunctions instance = new MapFunctions();

	private MapFunctions() {
		super(0, 2, "getAllMapNames", "getCurrentMapName", "getVisibleMapNames", "setCurrentMap",
				"getMapVisible", "setMapVisible", "setMapName");
	}

	public static MapFunctions getInstance() {
		return instance;
	}

	@Override
	public Object childEvaluate(Parser parser, String functionName, List<Object> parameters) throws ParserException {
		if (functionName.equals("getCurrentMapName")) {
			checkNumberOfParameters(functionName, parameters, 0, 0);
			return MapTool.getFrame().getCurrentZoneRenderer().getZone().getName();
		} else if (functionName.equals("setCurrentMap")) {
			checkNumberOfParameters(functionName, parameters, 1, 1);
			String mapName = parameters.get(0).toString();
			ZoneRenderer zr = getNamedMap(functionName, mapName);
			if (zr != null) {
				MapTool.getFrame().setCurrentZoneRenderer(zr);
				return mapName;
			}
			throw new ParserException(I18N.getText("macro.function.moveTokenMap.unknownMap", functionName, mapName));
			/*
			for (ZoneRenderer zr : MapTool.getFrame().getZoneRenderers()) {
				if (mapName.equals(zr.getZone().getName())) {
					MapTool.getFrame().setCurrentZoneRenderer(zr);
					return mapName;
				} // endif
			} // endfor */
		} else if ("getMapVisible".equalsIgnoreCase(functionName)) {
			checkNumberOfParameters(functionName, parameters, 0, 1);
			if (parameters.size() > 0) {
				String mapName = parameters.get(0).toString();
				return getNamedMap(functionName, mapName).getZone().isVisible() ? "1" : "0";
			} else {
				// Return the visibility of the current map/zone
				return MapTool.getFrame().getCurrentZoneRenderer().getZone().isVisible() ? "1" : "0";
			}

		} else if ("setMapVisible".equalsIgnoreCase(functionName)) {
			checkNumberOfParameters(functionName, parameters, 1, 2);
			boolean visible = AbstractTokenAccessorFunction.getBooleanValue(parameters.get(0).toString());
			Zone zone = MapTool.getFrame().getCurrentZoneRenderer().getZone();
			if (parameters.size() > 1) {
				String mapName = parameters.get(1).toString();
				zone = getNamedMap(functionName, mapName).getZone();
			}
			// Set the zone and return the visibility of the current map/zone
			zone.setVisible(visible);
			MapTool.serverCommand().setZoneVisibility(zone.getId(), zone.isVisible());
			MapTool.getFrame().getZoneMiniMapPanel().flush();
			MapTool.getFrame().repaint();
			return zone.isVisible() ? "1" : "0";

		} else if ("setMapName".equalsIgnoreCase(functionName)) {
			checkNumberOfParameters(functionName, parameters, 2, 2);
			String oldMapName = parameters.get(0).toString();
			String newMapName = parameters.get(1).toString();
			Zone zone = getNamedMap(functionName, oldMapName).getZone();
			zone.setName(newMapName);
			MapTool.serverCommand().renameZone(zone.getId(), newMapName);
			if (zone == MapTool.getFrame().getCurrentZoneRenderer().getZone())
				MapTool.getFrame().setCurrentZoneRenderer(MapTool.getFrame().getCurrentZoneRenderer());
			return zone.getName();

		} else {
			checkNumberOfParameters(functionName, parameters, 0, 1);
			boolean allMaps = functionName.equals("getAllMapNames");

			if (allMaps && !MapTool.getParser().isMacroTrusted()) {
				throw new ParserException(I18N.getText("macro.function.general.noPerm", functionName));
			}
			List<String> mapNames = new LinkedList<String>();
			for (ZoneRenderer zr : MapTool.getFrame().getZoneRenderers()) {
				if (allMaps || zr.getZone().isVisible()) {
					mapNames.add(zr.getZone().getName());
				}
			}
			String delim = parameters.size() > 0 ? parameters.get(0).toString() : ",";
			if ("json".equals(delim)) {
				return JSONArray.fromObject(mapNames);
			} else {
				return StringFunctions.getInstance().join(mapNames, delim);
			}
		}
	}

	/**
	 * Find the map/zone for a given map name
	 * @param functionName String Name of the calling function.
	 * @param mapName      String Name of the searched for map.
	 * @return             ZoneRenderer The map/zone.
	 * @throws ParserException  if the map is not found
	 */
	private ZoneRenderer getNamedMap(String functionName, String mapName) throws ParserException {
		for (ZoneRenderer zr : MapTool.getFrame().getZoneRenderers()) {
			if (mapName.equals(zr.getZone().getName())) {
				return zr;
			}
		}
		throw new ParserException(I18N.getText("macro.function.moveTokenMap.unknownMap", functionName, mapName));
	}

	/**
	 * Checks that the number of objects in the list <code>parameters</code>
	 * is within given bounds (inclusive). Throws a <code>ParserException</code>
	 * if the check fails.
	 *
	 * @param    functionName    this is used in the exception message
	 * @param    parameters      a list of parameters
	 * @param    min             the minimum amount of parameters (inclusive)
	 * @param    max             the maximum amount of parameters (inclusive)
	 * @throws   ParserException    if there were more or less parameters than allowed
	 */
	private void checkNumberOfParameters(String functionName, List<Object> parameters, int min, int max) throws ParserException {
		int numberOfParameters = parameters.size();
		if (numberOfParameters < min) {
			throw new ParserException(I18N.getText("macro.function.general.notEnoughParam", functionName, min, numberOfParameters));
		} else if (numberOfParameters > max) {
			throw new ParserException(I18N.getText("macro.function.general.tooManyParam", functionName, max, numberOfParameters));
		}
	}
}
