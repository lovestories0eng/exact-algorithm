package utils.impl;

import common.BatchLinker;
import global.Config;
import models.MetaPath;
import utils.InitialGraphConstructor;

import java.util.*;

// 从查询点扩展构建初始图
public class QueryNodeExpandStrategy implements InitialGraphConstructor {
    private int[][] graph = null;//data graph, including vertex IDs, edge IDs, and their link relationships
    private int[] vertexType = null;//vertex -> type
    private int[] edgeType = null;//edge -> type
    private int[] edgeUsedTimes = null;//edge -> used times
    private HashMap<Integer, Set<Integer>> homoGraph = new HashMap<>();
    Set<Integer> vertexFound = new HashSet<>();
    HashMap<Integer, ArrayList<Integer>> pathRecordMap = new HashMap<>();
    Set<Integer> keepSet = null;
    HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge = null;
    // path conflict
    HashMap<ArrayList<Integer>, Double> pathMapConflict = new HashMap<>();
    // <vertex pair -> path with lowest conflict>
    HashMap<Map.Entry<Integer, Integer>, ArrayList<Integer>> vertexPairMapPath = new HashMap<>();
    // <vertex pair -> path conflict>
    HashMap<Map.Entry<Integer, Integer>, Double> vertexPairMapConflict = new HashMap<>();

    public QueryNodeExpandStrategy(int[][] graph, int[] vertexType, int[] edgeType, int[] edgeUsedTimes, HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge) {
        this.graph = graph;
        this.vertexType = vertexType;
        this.edgeType = edgeType;
        this.edgeUsedTimes = edgeUsedTimes;
        this.vertexPairMapEdge = vertexPairMapEdge;
    }

    public int[][] query(int queryId, MetaPath metaPath) {
        if (metaPath.vertex[0] != vertexType[queryId]) {
            System.out.println("查询节点类型与元路径类型不匹配！");
            return null;
        }

        Set<Integer> newFoundVertex = new HashSet<>();
        Set<Integer> foundVertex = new HashSet<>();

        // 查询节点直接设置为已经找到
        vertexFound.add(queryId);
        foundVertex.add(queryId);

        // step 1: compute the connected subgraph via batch-search with labeling (BSL)
        BatchLinker batchLinker = new BatchLinker(graph, vertexType, edgeType);
        keepSet = batchLinker.link(queryId, metaPath);

        System.out.println("Break point!");


        // step 2: expand the graph from the new-found node, find possibly linked vertex
        newFoundVertex = this.ontHopTraverse(foundVertex, metaPath);

        // 存储所有路径
        ArrayList<ArrayList<Integer>> allPaths = new ArrayList<>();

        for (int vertexStart : foundVertex) {
            // 存储从单个点开始的所有路径
            Queue<ArrayList<Integer>> path = new LinkedList<>();
            Queue<Integer> queue = new LinkedList<>();

            ArrayList<Integer> temp = new ArrayList<>();
            temp.add(vertexStart);
            path.offer(temp);
            queue.offer(vertexStart);
            while (!queue.isEmpty()) {
                int tempVertex = queue.poll();;
                ArrayList<Integer> tempPath = path.poll();;

                if (pathRecordMap.get(tempVertex) == null) {
                    allPaths.add(tempPath);
                } else {
                    ArrayList<Integer> tmpArr = pathRecordMap.get(tempVertex);
                    for (Integer integer : tmpArr) {
                        queue.offer(integer);
                        assert tempPath != null;
                        ArrayList<Integer> newArr = new ArrayList<>(tempPath);
                        newArr.add(integer);
                        path.offer(newArr);
                    }
                }
            }
        }
        // System.out.println("Break point!");

        // step 3: link edges between new-found vertex set and original vertex set according to the pair conflict rule
        for (ArrayList<Integer> path : allPaths) {
            pathMapConflict.put(path, calcPathConflict(path));
        }

        for (Map.Entry<ArrayList<Integer>, Double> entry : pathMapConflict.entrySet()) {
            ArrayList<Integer> key = entry.getKey();
            double conflict = entry.getValue();
            int k = key.get(0);
            int v = key.get(key.size() - 1);
            if (vertexPairMapConflict.containsKey(Map.entry(k, v))) {
                if (conflict < vertexPairMapConflict.get(Map.entry(k, v))) {
                    vertexPairMapConflict.put(Map.entry(k, v), conflict);
                    vertexPairMapPath.put(Map.entry(k, v), key);
                }
            } else {
                vertexPairMapConflict.put(Map.entry(k, v), conflict);
                vertexPairMapPath.put(Map.entry(k, v), key);
            }
        }

        // link edges with the lowest conflict
        Map.Entry<Integer, Integer> tmp = null;
        double conflict = Double.POSITIVE_INFINITY;
        for (Map.Entry<Map.Entry<Integer, Integer>, Double> entry : vertexPairMapConflict.entrySet()) {
            if (entry.getValue() < conflict) {
                tmp = entry.getKey();
            }
        }

        ArrayList<Integer> path = vertexPairMapPath.get(tmp);
        linkHomoGraph(path);
        // update path conflict and vertex pair conflict
        updateVertexPairMap(path);

        System.out.println("Break point!");

        // step 4: link edges inner the new-found vertex
        return null;
    }

    private void linkHomoGraph(ArrayList<Integer> path) {
        if (homoGraph.containsKey(path.get(0))) {
            Set<Integer> set = homoGraph.get(path.get(0));
            set.add(path.get(path.size() - 1));
        } else {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(path.get(path.size() - 1));
            homoGraph.put(path.get(0), newSet);
        }

        if (homoGraph.containsKey(path.get(path.size() - 1))) {
            Set<Integer> set = homoGraph.get(path.get(path.size() - 1));
            set.add(path.get(0));
        } else {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(path.get(0));
            homoGraph.put(path.get(path.size() - 1), newSet);
        }

        // 更新边容量
        for (int i = 0; i < path.size() - 1; i++) {
            edgeUsedTimes[vertexPairMapEdge.get(Map.entry(path.get(i), path.get(i + 1)))]--;
        }

        // 如果点对已连接则去除其在HashMap中的键值对
        int k = path.get(0);
        int v = path.get(path.size() - 1);
        vertexPairMapConflict.remove(Map.entry(k, v));
        vertexPairMapPath.remove(Map.entry(k, v));
    }

    private void updateVertexPairMap(ArrayList<Integer> path) {

    }

    private double calcPathConflict(ArrayList<Integer> path) {
        double totalTimes = Config.SHARED_TIMES;

        double usedTimesInTotal = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            usedTimesInTotal += totalTimes - edgeUsedTimes[vertexPairMapEdge.get(Map.entry(path.get(i), path.get(i + 1)))];
        }
        return usedTimesInTotal / (totalTimes * (path.size() - 1));
    }

    private Set<Integer> ontHopTraverse(Set<Integer> startSet, MetaPath metaPath) {
        int pathLen = metaPath.pathLen;
        Set<Integer> batchSet = startSet;

        pathRecordMap = new HashMap<>();

        for (int index = 0; index < pathLen; index++) {
            int targetVType = metaPath.vertex[index + 1], targetEType = metaPath.edge[index];

            Set<Integer> nextBatchSet = new HashSet<>();
            for (int anchorId : batchSet) {
                int[] nbArr = graph[anchorId];
                for (int i = 0; i < nbArr.length; i += 2) {
                    int nbVertexId = nbArr[i], nbEdgeId = nbArr[i + 1];
                    if (targetVType == vertexType[nbVertexId]
                            && targetEType == edgeType[nbEdgeId]
                            && edgeUsedTimes[nbEdgeId] > 0
                            && !vertexFound.contains(nbVertexId)
                    ) {
                        if (index == metaPath.pathLen - 1) {
                            if (keepSet.contains(nbVertexId)) {
                                nextBatchSet.add(nbVertexId);
                                recordPath(pathRecordMap, anchorId, nbVertexId);
                            }
                        } else {
                            nextBatchSet.add(nbVertexId);
                            recordPath(pathRecordMap, anchorId, nbVertexId);
                        }
                    }
                }
            }
            batchSet = nextBatchSet;
        }

        return batchSet;
    }

    private void recordPath(HashMap<Integer, ArrayList<Integer>> pathRecordMap, int anchorId, int nbVertexId) {
        if (!pathRecordMap.containsKey(anchorId)) {
            ArrayList<Integer> vertexLinker = new ArrayList<>();
            vertexLinker.add(nbVertexId);
            pathRecordMap.put(anchorId, vertexLinker);
        } else {
            ArrayList<Integer> vertexLinkerPointer = pathRecordMap.get(anchorId);
            vertexLinkerPointer.add(nbVertexId);
        }
    }
}
