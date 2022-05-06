package de.esymetric.jerusalem.osmDataRepresentation;

import java.util.List;
import java.util.Map;

public class OSMRelation {
	public int id;
	public List<OSMRelationMember> members;
	public Map<String, String> tags;
}
