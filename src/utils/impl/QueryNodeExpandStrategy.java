package utils.impl;

import common.BatchLinker;
import models.MetaPath;
import utils.InitialGraphConstructor;

import java.util.*;

// 从查询点扩展构建初始图
public class QueryNodeExpandStrategy implements InitialGraphConstructor {
    private int[][] graph = null;//data graph, including vertex IDs, edge IDs, and their link relationships
    private int[] vertexType = null;//vertex -> type
    private int[] edgeType = null;//edge -> type
    private int[] edgeUsedTimes = null;//edge -> used times
    Set<Integer> vertexFound = new HashSet<>();
    HashMap<Integer, ArrayList<Integer>> pathRecordMap = new HashMap<>();
    Set<Integer> keepSet = null;

    public QueryNodeExpandStrategy(int[][] graph, int[] vertexType, int[] edgeType, int[] edgeUsedTimes) {
        this.graph = graph;
        this.vertexType = vertexType;
        this.edgeType = edgeType;
        this.edgeUsedTimes = edgeUsedTimes;
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
        // <vertex pair -> path>
        HashMap<Map.Entry<Integer, Integer>, ArrayList<ArrayList<Integer>>> vertexPairMapPath = new HashMap<>();
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
        System.out.println("Break point!");



        // step 3: link edges between new-found vertex set and original vertex set according to the pair conflict rule
        // path conflict
        HashMap<ArrayList<Integer>, Integer> pathMapConflict = new HashMap<>();
        // vertex pair conflict
        HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapConflict = new HashMap<>();

        for (int i = 0; i < allPaths.size(); i++) {
            ArrayList<Integer> path = allPaths.get(i);
            pathMapConflict.put(path, calcPathConflict(path));
        }

        // step 4: link edges inner the new-found vertex
        return null;
    }

    private int calcPathConflict(ArrayList<Integer> path) {
        return 0;
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
