package utils.impl;

import common.BatchLinker;
import edgeDisjoint.MaxFlow;
import global.Config;
import models.MetaPath;

import java.util.*;

/**
 * 运行时间：70916
 * 边总使用次数：844908.0
 * 共享次数使用百分比：0.07216961381379443
 * 同构图点数：44283
 * 同构图边数：422454
 *
 * 运行时间：41548
 * 边总使用次数：858442.0
 * 共享次数使用百分比：0.07332564920860178
 * 同构图点数：44283
 * 同构图边数：429221
 *
 **/

public class MaxFlowStrategy {
    private final int[][] graph;//data graph, including vertex IDs, edge IDs, and their link relationships
    private final int[] vertexType;//vertex -> type
    private final int[] edgeType;//edge -> type
    private final int[] edgeUsedTimes;//edge -> used times
    private final HashMap<Integer, Set<Integer>> homoGraph = new HashMap<>();
    Set<Integer> keepSet = null;
    Set<Integer> remainSet = null;
    HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge;
    int pathLen = 0;

    public MaxFlowStrategy(int[][] graph, int[] vertexType, int[] edgeType, int[] edgeUsedTimes, HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge) {
        this.graph = graph;
        this.vertexType = vertexType;
        this.edgeType = edgeType;
        this.edgeUsedTimes = edgeUsedTimes;
        this.vertexPairMapEdge = vertexPairMapEdge;
        this.remainSet = new HashSet<>();
    }

    public int[][] query(int queryId, MetaPath metaPath) {
        this.pathLen = metaPath.pathLen + 1;
        if (metaPath.vertex[0] != vertexType[queryId]) {
            System.out.println("查询节点类型与元路径类型不匹配！");
            return null;
        }

        // step 1: compute the connected subgraph via batch-search with labeling (BSL)
        BatchLinker batchLinker = new BatchLinker(graph, vertexType, edgeType);
        keepSet = batchLinker.link(queryId, metaPath);

        //step 2: perform pruning
        // 根据路径的第一条边删除了一遍出度<k的节点
        // SECONDVertexTYPE: P, SECONDEdgeTYPE: A -> P
        int SECONDVertexTYPE = metaPath.vertex[1], SECONDEdgeTYPE = metaPath.edge[0];
        Iterator<Integer> keepIter = keepSet.iterator();
        while(keepIter.hasNext()) {
            int id = keepIter.next();
            int count = 0;
            // 遍历所有与查询节点p邻居的点
            for(int i = 0;i < graph[id].length;i += 2) {
                int nbVId = graph[id][i], nbEId = graph[id][i + 1];
                if(vertexType[nbVId] == SECONDVertexTYPE && edgeType[nbEId] == SECONDEdgeTYPE) {
                    count ++;
                    if(count >= Config.k - 1) break;
                }
            }
            if(count < Config.k - 1) keepIter.remove();
        }
        if(!keepSet.contains(queryId)) return null;

        long startTime = System.currentTimeMillis();
        // TODO: 如果边已经在同构图中存在，则使其到虚拟锚点的容量变为1
        MaxFlow maxFlow = new MaxFlow(graph, vertexType, edgeType, edgeUsedTimes, metaPath, homoGraph);
        Map<Integer, int[]> pathMap = new HashMap<>();
        int count = 0;
        // 对所有的点运用网络最大流算法
        for (int vid : keepSet) {
            count++;
            System.out.println(count);
            remainSet.add(vid);
            pathMap = maxFlow.obtainEPaths(vid, keepSet, pathMap);// invoke the exact algorithm
            for (Map.Entry<Integer, int[]> integerEntry: pathMap.entrySet()) {
                int vertex = integerEntry.getKey();
                int[] edges = integerEntry.getValue();
                // 更新边使用次数
                for (int edge : edges) {
                    edgeUsedTimes[edge]--;
                }
                remainSet.add(vertex);
                homeGraphBuilder(vid, vertex);
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("运行时间：" + (endTime - startTime));

        double totalUsedTimes = 0;
        for (Map.Entry<Integer, Set<Integer>> integerSetEntry : homoGraph.entrySet()) {
            Set<Integer> adjacent = integerSetEntry.getValue();
            totalUsedTimes += adjacent.size() * metaPath.pathLen;
        }

        totalUsedTimes /= 2;

        double usedPercent = totalUsedTimes / (Config.SHARED_TIMES * edgeUsedTimes.length);
        System.out.println("边总使用次数：" + totalUsedTimes);
        System.out.println("共享次数使用百分比：" + usedPercent);

        int vertexInTotal = 0;
        int edgeInTotal = 0;
        Map.Entry<Integer, Set<Integer>> entry;
        for (Map.Entry<Integer, Set<Integer>> integerSetEntry : homoGraph.entrySet()) {
            entry = integerSetEntry;
            vertexInTotal++;
            edgeInTotal += entry.getValue().size();
        }

        edgeInTotal /= 2;
        System.out.println("同构图点数：" + vertexInTotal);
        System.out.println("同构图边数：" + edgeInTotal);

        TrussDecomposition trussDecomposition = new TrussDecomposition(homoGraph);
        trussDecomposition.executeDecompose();

        return null;
    }

    private void homeGraphBuilder(int startPoint, int endPoint) {
        Set<Integer> set = homoGraph.get(startPoint);
        if (set != null) {
            set.add(endPoint);
        } else {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(endPoint);
            homoGraph.put(startPoint, newSet);
        }

        set = homoGraph.get(endPoint);
        if (set != null) {
            set.add(startPoint);
        } else {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(startPoint);
            homoGraph.put(endPoint, newSet);
        }
    }
}
