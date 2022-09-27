package edgeDisjoint;


import global.Config;
import models.MetaPath;

import java.util.*;

/**
 * @author pan shihuang
 * @date 26 Nov. 2022
 * The max-flow-based algorithm for computing the e-degree with capacity constraint
 */
public class MaxFlow {
    private int[][] graph = null;//data graph, including vertex IDs, edge IDs, and their link relationships
    private int[] vertexType = null;//vertex -> type
    private int[] edgeType = null;//edge -> type
    private int[] edgeUsedTimes = null;
    private MetaPath queryMPath = null;//the query meta-path
    // vertex id -> <vertex id, capacity>
    private Map<Integer, HashMap<Integer, Integer>> flowGraph = null;
    private List<Set<Integer>> vertexList = null;
    HashMap<Integer, Set<Integer>> homoGraph = null;

    public MaxFlow(int[][] graph, int[] vertexType, int[] edgeType, int[] edgeUsedTimes, MetaPath queryPath, HashMap<Integer, Set<Integer>> homoGraph) {
        this.graph = graph;
        this.vertexType = vertexType;
        this.edgeType = edgeType;
        this.edgeUsedTimes = edgeUsedTimes;
        this.queryMPath = queryPath;
        this.homoGraph = homoGraph;
    }

    public int obtainEDegree(int vertexId, Set<Integer> keepSet, Map<Integer, int[]> pathMap) {
        return obtainENeighbors(vertexId, keepSet, pathMap).size();
    }

    public Set<Integer> obtainENeighbors(int vertexId, Set<Integer> keepSet, Map<Integer, int[]> pathMap) {
        flowGraph = null;
        vertexList = null;

        //step 1: create the flow network
        createFlowGraph(vertexId, keepSet);
        if (flowGraph == null) return new HashSet<Integer>();

        //step 2: find the neighbor by finding an argument path
        if (pathMap != null) {
            return collectENeighbors(vertexId, pathMap.keySet());
        } else {
            return collectENeighbors(vertexId, null);
        }
    }

    private Set<Integer> collectENeighbors(int vertexId, Set<Integer> existSet) {
        Set<Integer> nbSet = new HashSet<Integer>();
        if (existSet != null) {
            for (int id : existSet) {
                nbSet.add(id);
            }
        }
        int neighborId = augOnePathNeighbor(vertexId, -1);
        while (neighborId != -1) {
            nbSet.add(neighborId);
            neighborId = augOnePathNeighbor(vertexId, -1);
        }
        return nbSet;
    }

    // Notice: this method is added on Oct 31
    public Map<Integer, int[]> obtainEPaths(int vertexId, Set<Integer> keepSet, Map<Integer, int[]> pathMap) {
        flowGraph = null;
        vertexList = null;

        // 排除不是P邻居的点
        Set<Integer> removeSet = new HashSet<>();
        for (int endPoint : pathMap.keySet()) {
            if (!keepSet.contains(endPoint)) {
                removeSet.add(endPoint);
            }
        }
		/*if (removeSet.isEmpty() && !pathMap.isEmpty()) {
			return pathMap;
		}*/
        pathMap.keySet().removeAll(removeSet);

        //step 1: create the flow network
        createFlowGraph(vertexId, keepSet);
        if (flowGraph == null) return new HashMap<>();

        int neighBorId = augOnePathNeighbor(vertexId, -1);
        while (neighBorId != -1) {
            neighBorId = augOnePathNeighbor(vertexId, -1);
        }

        //step 2: find the edge-disjoint paths from the flow network
        Map<Integer, int[]> maxFlowPathMap = new HashMap<>();
        for (Map.Entry<Integer, Integer> integerIntegerEntry : flowGraph.get(-1).entrySet()) {
            int vid = integerIntegerEntry.getKey();

            int[] vPath = obtainOnePath(vid, vertexId);

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
        for (int i = 0; i < edgePath.length; i++) {
            int curVid = vertexPath[i];
            for (int j = 0; j < graph[curVid].length; j = j + 2) {
                if (graph[curVid][j] == vertexPath[i + 1]) {
                    edgePath[i] = graph[curVid][j + 1];
                }
            }
        }
        return edgePath;
    }

    // find an edge-disjoint path
    // TODO: make it right for the new method
    private int[] obtainOnePath(int vid, int tid) {
        Stack<Integer> stack = new Stack<>();
        Map<Integer, Set<Integer>> visitMap = new HashMap<>();
        stack.push(vid);

        int requiredLen = queryMPath.vertex.length;
        for (int i = 0; i < requiredLen; i++) {
            Set<Integer> newSet = new HashSet<>();
            visitMap.put(i, newSet);
        }
        visitMap.get(0).add(vid);
        while (!stack.isEmpty()) {
            boolean stackPush = false;
            for (Map.Entry<Integer, Integer> entry : flowGraph.get(stack.peek()).entrySet()) {
                if (stack.size() < requiredLen) {
                    int v = entry.getKey();
                    int flow = entry.getValue();
                    if (!visitMap.get(stack.size()).contains(v) && flow > 0) {
                        stackPush = true;
                        visitMap.get(stack.size()).add(v);
                        stack.push(v);
                        break;
                    }
                    // System.out.println("Key:" + entry.getKey() + " value:" + entry.getValue());
                }
            }
            if (stack.size() == requiredLen && stack.peek() == tid) {
                break;
            }
            if (!stackPush) {
                stack.pop();
            }
        }
        if (stack.isEmpty()) {// there is no meta-path
            return null;
        }
        // 如果容量小于等于零则删除边
        for (int i = 0; i < stack.size() - 1; i++) {
            int curFlow = flowGraph.get(stack.get(i)).get(stack.get(i + 1));
            flowGraph.get(stack.get(i)).put(stack.get(i + 1), curFlow - 1);
            curFlow = flowGraph.get(stack.get(i)).get(stack.get(i + 1));
            if (curFlow <= 0)
                flowGraph.get(stack.get(i)).remove(stack.get(i + 1));
        }

        int[] result = new int[queryMPath.vertex.length];
        for (int i = result.length - 1; i >= 0; i--) {
            result[i] = stack.get(result.length - 1 - i) % graph.length;
        }
        return result;
    }

    //find an e-neighbor from an s-t path in the flow network
    private int augOnePathNeighbor(int s, int t) {
        Set<Integer> visitSet = new HashSet<>();
        Stack<Integer> stack = new Stack<>();
        stack.push(s);
        visitSet.add(s);
        while (!stack.isEmpty()) {
            boolean stackPush = false;
            for (Map.Entry<Integer, Integer> entry : flowGraph.get(stack.peek()).entrySet()) {
                int v = entry.getKey();
                int flow = entry.getValue();
                if (!visitSet.contains(v) && flow > 0) {
                    stackPush = true;
                    visitSet.add(v);
                    stack.push(v);
                    break;
                }
                // System.out.println("Key:" + entry.getKey() + " value:" + entry.getValue());
            }

            if (stack.peek() == t) {
                break;
            }
            if (!stackPush) {
                stack.pop();
            }
        }
        if (stack.isEmpty()) {
            return -1; //there is no path
        }

        // update the directions of edges in the flow network
        // 更新网络流量
        for (int i = 0; i < stack.size() - 1; i++) {
            int curFlowForward = flowGraph.get(stack.get(i)).get(stack.get(i + 1));
            int curFlowBackward = 0;
            if (flowGraph.get(stack.get(i + 1)).get(stack.get(i)) != null)
                curFlowBackward = flowGraph.get(stack.get(i + 1)).get(stack.get(i));
            else {
                flowGraph.get(stack.get(i + 1)).put(stack.get(i), 0);
            }
            flowGraph.get(stack.get(i)).put(stack.get(i + 1), curFlowForward - 1);
            flowGraph.get(stack.get(i + 1)).put(stack.get(i), curFlowBackward + 1);
        }
        int result = stack.get(stack.size() - 2) >= graph.length ? stack.get(stack.size() - 2) - graph.length : stack.get(stack.size() - 2);
        return result;
    }

    // 从起始节点vertexId开始根据元路径收集到所有关联的点
    private void collectVertices(int vertexId, Set<Integer> keepSet) {
        // step 1: collect vertices from left to right
        // 第一个集合代表元路径第一种类型的节点，第二个集合代表元路径第二种类型的节点，。。。依此类推
        vertexList = new ArrayList<>();
        Set<Integer> v0Set = new HashSet<>();
        v0Set.add(vertexId);
        vertexList.add(v0Set);
        for (int i = 0; i < queryMPath.pathLen; i++) {
            Set<Integer> curSet = new HashSet<>();
            for (int vid : vertexList.get(i)) {
                for (int k = 0; k < graph[vid].length; k = k + 2) {
                    int tmpVId = graph[vid][k], tmpEId = graph[vid][k + 1];
                    if (vertexType[tmpVId] == queryMPath.vertex[i + 1] && edgeType[tmpEId] == queryMPath.edge[i]) {
                        if (i < queryMPath.pathLen - 1) {
                            curSet.add(tmpVId);
                        } else {
                            if (keepSet.contains(tmpVId)) {
                                curSet.add(tmpVId);
                            }
                        }
                    }
                }
            }
            vertexList.add(curSet);
        }

        // 去除起始节点，因为起始节点不可能是虚拟锚点
        vertexList.get(queryMPath.pathLen).remove(vertexId); //the source node and sink node are different

        // 来回遍历两遍是因为DBLP提供的是有向图（个人理解）
        //step 2: collect vertices from right to left
        for (int i = queryMPath.pathLen; i > 0; i--) {
            Set<Integer> newSet = new HashSet<>();
            for (int vid : vertexList.get(i)) {
                for (int k = 0; k < graph[vid].length; k = k + 2) {
                    if (vertexList.get(i - 1).contains(graph[vid][k])) {
                        newSet.add(graph[vid][k]);
                    }
                }
            }
            vertexList.set(i - 1, newSet);
        }
    }

    //transfer the VertexPath to newID
    private Set<int[]> getNewIdPathSet(int vertexId, Map<Integer, int[]> pathsMap, int NUM) {
        Set<int[]> newIdPathSet = new HashSet<>();
        for (int vid : pathsMap.keySet()) {
            int[] ePath = pathsMap.get(vid);
            int[] vPath = getVPath(vertexId, ePath);
            int[] newIdPath = getNewIdPath(vPath, NUM);
            newIdPathSet.add(newIdPath);
        }
        return newIdPathSet;
    }

    private int[] getVPath(int source, int[] edgePath) {
        int[] vertexPath = new int[edgePath.length + 1];
        vertexPath[0] = source;
        for (int i = 0; i < edgePath.length; i++) {
            int curVid = vertexPath[i];
            for (int j = 1; j < graph[curVid].length; j = j + 2) {
                if (graph[curVid][j] == edgePath[i]) {
                    vertexPath[i + 1] = graph[curVid][j - 1];
                }
            }
        }
        return vertexPath;
    }

    private int[] getNewIdPath(int[] vPath, int NUM) {
        int[] newIdPath = new int[vPath.length + 1];
        newIdPath[0] = vPath[0];
        for (int i = 1; i < vPath.length; i++) {
            if (flowGraph.get(newIdPath[i - 1]).containsKey(vPath[i])) {
                newIdPath[i] = vPath[i];
            } else if (flowGraph.get(newIdPath[i - 1]).containsKey(vPath[i] + NUM)) {
                newIdPath[i] = vPath[i] + NUM;
            }
        }
        for (int i = 1; i < vPath.length; i++) {
            // <vertex -> capacity>
            HashMap<Integer, Integer> neighborMap = flowGraph.get(newIdPath[i - 1]);
            // TODO: 判断容量 > 0
            if (neighborMap.containsKey(vPath[i]))
                newIdPath[i] = vPath[i];
            else if (neighborMap.containsKey(vPath[i] + NUM))
                newIdPath[i] = vPath[i] + NUM;
        }
        newIdPath[vPath.length] = -1;
        return newIdPath;
    }

    private void createFlowGraph(int vertexId, Set<Integer> keepSet) {
        //step 1: collect vertices from left -> right and right -> left
        collectVertices(vertexId, keepSet);
        if (vertexList.get(queryMPath.pathLen).size() == 0) return;

        //step 2: create the flow network for the first (pathLen -1)-th layers
        // 引入可共享次数的流量图
        flowGraph = new HashMap<Integer, HashMap<Integer, Integer>>();
        // 用于判断是正向还是反向，因为网络最大流有正向和反向
        int NUMBER = graph.length;
        for (int i = 0; i < vertexList.size() - 1; i++) {
            // TODO: simulate process for better understanding
            for (int v : vertexList.get(i)) {
                // <vertex -> capacity>
                HashMap<Integer, Integer> neighborMap = new HashMap<>();

                // 元路径下一类型的节点集合
                Set<Integer> tmpSet = vertexList.get(i + 1);
                for (int nid = 0; nid < graph[v].length; nid += 2) {
                    int sharedTime = edgeUsedTimes[graph[v][nid + 1]];
                    // 判断点v的邻接链表中的点是否属于元路径下一类型的节点集合
                    if (tmpSet.contains(graph[v][nid])) {
                        // 如果边的容量大于0
                        if (sharedTime > 0) {
                            if (flowGraph.containsKey(graph[v][nid])) {
                                // neighborMap.put(graph[v][nid] + NUMBER, Config.SHARED_TIMES);
                                neighborMap.put(graph[v][nid] + NUMBER, sharedTime);
                            } else {
                                // neighborMap.put(graph[v][nid], Config.SHARED_TIMES);
                                neighborMap.put(graph[v][nid], sharedTime);
                            }
                        }
                    }
                }
                if (flowGraph.containsKey(v)) {
                    flowGraph.put(v + NUMBER, neighborMap);
                } else {
                    flowGraph.put(v, neighborMap);
                }
            }
        }

        //step 3: create the flow network for the pathLen-th layer
        for (int v : vertexList.get(vertexList.size() - 1)) {
            HashMap<Integer, Integer> neighborMap = new HashMap<>();
            // 到虚拟锚点的容量始终设置为1
            // 如果同构图中有边则跳过
            Set<Integer> neighbours = homoGraph.get(v);
            if (neighbours == null || !neighbours.contains(vertexId))
                neighborMap.put(-1, 1);
            if (flowGraph.containsKey(v)) {
                flowGraph.put(v + NUMBER, neighborMap);
            } else {
                flowGraph.put(v, neighborMap);
            }
        }

        //step 4: create the sink node
        HashMap<Integer, Integer> neighborMap = new HashMap<>();
        flowGraph.put(-1, neighborMap);
        // System.out.println("Break point!");
    }
}
