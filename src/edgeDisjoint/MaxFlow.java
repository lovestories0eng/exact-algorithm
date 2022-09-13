package edgeDisjoint;


import models.MetaPath;

import java.util.*;

/**
 * @author fangyixiang
 * @date 24 Sep. 2018
 * The max-flow-based algorithm for computing the e-degree
 */
public class MaxFlow {
	private int[][] graph = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int[] vertexType = null;//vertex -> type
	private int[] edgeType = null;//edge -> type
	private MetaPath queryMPath = null;//the query meta-path
	private Map<Integer, Set<Integer>> flowGraphMap = null;
	// vertex id -> <vertex id, capacity>
	private Map<Integer, Map<Integer, Integer>> flowGraph = null;
	private List<Set<Integer>> vertexList = null;
		
	public MaxFlow(int[][] graph, int[] vertexType, int[] edgeType, MetaPath queryPath) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
		this.queryMPath = queryPath;
	}
	
	public int obtainEDegree(int vertexId, Set<Integer> keepSet, Map<Integer, int[]> pathMap) {
		return obtainENeighbors(vertexId, keepSet, pathMap).size();
	}
	
	public Set<Integer> obtainENeighbors(int vertexId, Set<Integer> keepSet, Map<Integer, int[]> pathMap) {
		flowGraphMap = null;
		vertexList = null;
		
		//step 1: create the flow network
		createFlowGraph(vertexId, keepSet, pathMap);
		if(flowGraphMap == null)   return new HashSet<Integer>();
		
		//step 2: find the neighbor by finding an argument path
		if(pathMap != null) {
			return collectENeighbors(vertexId, pathMap.keySet());
		}else {
			return collectENeighbors(vertexId, null);
		}
	}
	
	private Set<Integer> collectENeighbors(int vertexId, Set<Integer> existSet) {
		Set<Integer> nbSet = new HashSet<Integer>();
		if(existSet != null) {
			for(int id:existSet) {
				nbSet.add(id);
			}
		}
		int neighborId = augOnePathNeighbor(vertexId, -1);
		while(neighborId != -1) {
			nbSet.add(neighborId);
			neighborId = augOnePathNeighbor(vertexId, -1);
		}
		return nbSet;
	}
	
	//Notice: this method is added on Oct 31
	public Map<Integer, int[]> obtainEPaths(int vertexId, Set<Integer> keepSet, Map<Integer, int[]> pathMap) {
		flowGraphMap = null;
		vertexList = null;
		
		Set<Integer> removeSet = new HashSet<Integer>();
		for (int endPoint : pathMap.keySet() ) {
			if ( !keepSet.contains(endPoint) ) {
				removeSet.add(endPoint);
			}
		}
		/*if (removeSet.isEmpty() && !pathMap.isEmpty()) {
			return pathMap;
		}*/
		pathMap.keySet().removeAll(removeSet);

		//step 1: create the flow network
		createFlowGraph(vertexId, keepSet, pathMap);
		if(flowGraphMap == null)   return new HashMap<Integer, int[]>();


		int neighBorId = augOnePathNeighbor(vertexId , -1);
		while(neighBorId != -1) {
			neighBorId = augOnePathNeighbor(vertexId , -1);
		}
		
		//step 2: find the edge-disjoint paths from the flow network
		Map<Integer, int[]> maxFlowPathMap = new HashMap<Integer , int[]>();
		for (int vid : flowGraphMap.get(-1)) {
			int[] vPath = obtainOnePath(vid , vertexId);//from right to left
		
			if (vPath != null) {
				int endVertex = vPath[vPath.length - 1];
				int[] ePath = getEPath(vPath);
				maxFlowPathMap.put(endVertex, ePath);
			}
		}
		return maxFlowPathMap;
	}

	private int[] getEPath(int[] vertexPath) {
		int[] edgePath = new int[vertexPath.length - 1];
		for (int i = 0 ; i < edgePath.length ; i++) {
			int curVid = vertexPath[i];
			for (int j = 0 ; j < graph[curVid].length ; j = j + 2) {
				if (graph[curVid][j] == vertexPath[i + 1]) {
					edgePath[i] = graph[curVid][j + 1];
				}
			}
		}
		return edgePath;
	}
	
	//find an edge-disjoint path
 	private int[] obtainOnePath(int vid , int tid) {
		Stack<Integer> stack = new Stack<Integer>();
		Map<Integer , Set<Integer> > visitMap = new HashMap<Integer , Set<Integer>>();
		stack.push(vid);
		
		int requiredLen = queryMPath.vertex.length;
		for (int i = 0 ; i < requiredLen ; i++) {
			Set<Integer> newSet = new HashSet<Integer>();
			visitMap.put(i, newSet);
		}
		visitMap.get(0).add(vid);
		while (!stack.isEmpty()) {
			boolean stackPush = false;
			for(int v : flowGraphMap.get(stack.peek())) {
				if ( stack.size() < requiredLen) {
					if (!visitMap.get(stack.size() ).contains(v)) {
						stackPush = true;
						visitMap.get(stack.size()).add(v);
						stack.push(v);
						break;
					}
				}
			}
			if(stack.size() == requiredLen && stack.peek() == tid) {
				break;
			}
			if(!stackPush) {
				stack.pop();
			}
		}
		if (stack.isEmpty()) {//there is no meta-path
			return null;
		}
		
		//delete this meta-path from the flow network
		for(int i = 0 ; i < stack.size() - 1 ; i++) {
			flowGraphMap.get(stack.get(i)).remove(stack.get(i+1));
		}
		int[] result = new int[queryMPath.vertex.length];
		for (int i = result.length - 1 ; i >= 0 ; i--) {
			result[i] = stack.get(result.length - 1 - i) % graph.length;
		}
		return result;
	}
	
	//find an e-neighbor from an s-t path in the flow network
	private int augOnePathNeighbor(int s, int t){
		Set<Integer> visitSet = new HashSet<Integer>();
		Stack<Integer> stack = new Stack<Integer>();
		stack.push(s);
		visitSet.add(s);
		while(!stack.isEmpty()) {
			boolean stackPush = false;
			for(int v : flowGraphMap.get(stack.peek())) {
				if (!visitSet.contains(v)) {
					stackPush = true;
					visitSet.add(v);
					stack.push(v);
					break;
				}
			}
			if(stack.peek() == t) {
				break;
			}
			if(!stackPush) {
				stack.pop();
			}
		}
		if(stack.isEmpty()) {
			return -1;//there is no path
		}
		
		//update the directions of edges in the flow network
		for(int i = 0 ; i < stack.size() - 1; i++) {
			flowGraphMap.get(stack.get(i)).remove(stack.get(i+1));
			flowGraphMap.get(stack.get(i+1)).add(stack.get(i));
		}
		int result = stack.get(stack.size() - 2) >= graph.length ? stack.get(stack.size() - 2) - graph.length : stack.get(stack.size() - 2);
		return result;
	}
	
	private void collectVertices(int vertexId, Set<Integer> keepSet) {
		//step 1: collect vertices from left to right
		vertexList = new ArrayList<Set<Integer>>();
		Set<Integer> v0Set = new HashSet<Integer>();
		v0Set.add(vertexId);
		vertexList.add(v0Set);
		for(int i = 0; i < queryMPath.pathLen; i++) {
			Set<Integer> curSet = new HashSet<Integer>();
			for(int vid: vertexList.get(i) ) {
				for(int k = 0; k < graph[vid].length; k = k + 2) {
					int tmpVId = graph[vid][k], tmpEId = graph[vid][k + 1];
					if (vertexType[tmpVId] == queryMPath.vertex[i + 1] 
							&& edgeType[tmpEId] == queryMPath.edge[i]) {
						if(i < queryMPath.pathLen - 1) {
							curSet.add(tmpVId);
						}else {
							if(keepSet.contains(tmpVId)) {
								curSet.add(tmpVId);
							}
						}
					}
				}
			}
			vertexList.add(curSet);
		}
		vertexList.get(queryMPath.pathLen).remove(vertexId);//the source node and sink node are different
		
		//step 2: collect vertices from right to left
		for (int i = queryMPath.pathLen; i > 0; i--) {
			Set<Integer> newSet = new HashSet<Integer>();
			for (int vid: vertexList.get(i)) {
				for (int k = 0; k < graph[vid].length; k = k + 2) {
					if (vertexList.get(i - 1).contains(graph[vid][k])) {
						newSet.add(graph[vid][k]);
					}
				}
			}
			vertexList.set(i - 1 , newSet);
		}
	}
	
	//transfer the VertexPath to newID
	private Set<int[]> getNewIdPathSet (int vertexId , Map<Integer , int[]> pathsMap , int NUM ) {
		Set<int[]> newIdPathSet = new HashSet<int[]>();
		for (int vid : pathsMap.keySet()) {
			int[] ePath = pathsMap.get(vid);
			int[] vPath = getVPath(vertexId , ePath);
			int[] newIdPath = getNewIdPath(vPath , NUM);
			newIdPathSet.add(newIdPath);
		}
		return newIdPathSet;
	}
	
	private int[] getVPath( int source , int[] edgePath) {
		int[] vertexPath = new int[edgePath.length + 1];
		vertexPath[0] = source;
		for (int i = 0 ; i < edgePath.length ; i++) {
			int curVid = vertexPath[i];
			for (int j = 1 ; j < graph[curVid].length ; j = j + 2) {
				if (graph[curVid][j] == edgePath[i]) {
					vertexPath[i + 1] = graph[curVid][j - 1];
				}
			}
		}
		return vertexPath;
	}
	
	private int[] getNewIdPath(int[] vPath , int NUM) {
		int[] newIdPath = new int[vPath.length + 1];
		newIdPath[0] = vPath[0];
		for (int i = 1 ; i < vPath.length ; i++) {
			if (flowGraphMap.get(newIdPath[i - 1] ).contains(vPath[i]) ) {
				newIdPath[i] = vPath[i];
			} else if (flowGraphMap.get(newIdPath[i - 1]).contains(vPath[i] + NUM)) {
				newIdPath[i] = vPath[i] + NUM;
			}
		}
		newIdPath[vPath.length] = -1;
		return newIdPath;
	}
	
	private void createFlowGraph(int vertexId, Set<Integer> keepSet, Map<Integer, int[]> pathMap) {
		//step 1: collect vertices from left -> right and right -> left
		collectVertices(vertexId, keepSet);
		if(vertexList.get(queryMPath.pathLen).size() == 0)   return ;
		
		//step 2: create the flow network for the first (pathLen -1)-th layers
		flowGraphMap = new HashMap<Integer, Set<Integer>>();
		int NUMBER = graph.length;
		for(int i = 0; i < vertexList.size() - 1; i++) {
			for(int v : vertexList.get(i)) {
				HashSet<Integer> neiborSet = new HashSet<Integer>();
				Set<Integer> tmpSet = vertexList.get(i + 1);
				for(int nid = 0 ; nid < graph[v].length; nid += 2) {
					if(tmpSet.contains(graph[v][nid])) {
						if (flowGraphMap.containsKey(graph[v][nid])) {
							neiborSet.add(graph[v][nid] + NUMBER);
						} else {
							neiborSet.add(graph[v][nid]);
						}
					}
				}
				if(flowGraphMap.containsKey(v)) {
					flowGraphMap.put(v + NUMBER, neiborSet);
				} else {
					flowGraphMap.put(v, neiborSet);
				}	
			}
		}
		
		//step 3: create the flow network for the pathLen-th layer
		for(int v : vertexList.get(vertexList.size() - 1)) {
			HashSet<Integer> neiborSet = new HashSet<Integer>();
			neiborSet.add(-1);//We use -1 to denote the sink node
			if(flowGraphMap.containsKey(v)) {
				flowGraphMap.put(v + NUMBER, neiborSet);
			}else {
				flowGraphMap.put(v, neiborSet);
			}	
		}
		
		//step 4: create the sink node
		Set<Integer> neiborSet = new HashSet<Integer>();
		flowGraphMap.put(-1, neiborSet);
		
		//step 5: consider the paths in pathMap
		if(pathMap != null) {
			Set<int[]> newIdPathSet = getNewIdPathSet(vertexId , pathMap, NUMBER);
			for (int[] newIdPath : newIdPathSet) {
				for (int i = 1 ; i < newIdPath.length ; i++) {
					flowGraphMap.get(newIdPath[i - 1]).remove(newIdPath[i]);
					flowGraphMap.get(newIdPath[i]).add(newIdPath[i - 1]);
				}
			}
		}
	}
}
