package edgeDisjoint;

import common.BatchLinker;
import models.MetaPath;

import java.util.*;

public class HomoBTruss {
    private int[][] graph = null;//data graph, including vertex IDs, edge IDs, and their link relationships
    private int[] vertexType = null;//vertex -> type
    private int[] edgeType = null;//edge -> type
    private int[][] graphCopy = null;


    public HomoBTruss(int[][] graph, int[] vertexType, int[] edgeType) {
        this.graph = graph;
        this.vertexType = vertexType;
        this.edgeType = edgeType;
        // 深拷贝
        for (int i = 0; i < graph.length; i++) {
            assert false;
            System.arraycopy(graph[i], 0, graphCopy[i], 0, graph[i].length);
        }

    }

    public Set<Integer> query(int queryId, MetaPath queryMPath, int queryK) {
        //step 0: check whether queryId's type matches with the meta-path
        if (queryMPath.vertex[0] != vertexType[queryId]) return null;


        //step 1: compute the connected subgraph via batch-search with labeling (BSL)
        BatchLinker batchLinker = new BatchLinker(graph, vertexType, edgeType);
        Set<Integer> keepSet = batchLinker.link(queryId, queryMPath);
        if (keepSet == null) return null;


        //step 2: perform pruning
        //根据路径的第一步删除了一遍出度 < k - 1的节点
        int SECONDVertexTYPE = queryMPath.vertex[1], SECONDEdgeTYPE = queryMPath.edge[0];
        Iterator<Integer> keepIter = keepSet.iterator();
        while (keepIter.hasNext()) {
            int id = keepIter.next();
            int count = 0;
            for (int i = 0; i < graph[id].length; i += 2) {
                int nbVId = graph[id][i], nbEId = graph[id][i + 1];
                if (vertexType[nbVId] == SECONDVertexTYPE && edgeType[nbEId] == SECONDEdgeTYPE) {
                    count++;
                    if (count >= queryK) break;
                }
            }
            if (count < queryK - 1) keepIter.remove();
        }
        if (!keepSet.contains(queryId)) return null;

        // TODO: execute max-flow algorithm for each element in the keepSet to build a homogeneous graph
        MaxFlow maxFlow = new MaxFlow(graph, vertexType, edgeType, queryMPath);
        for (int vid : keepSet) {
            Map<Integer, int[]> pathMap = new HashMap<Integer, int[]>();
            //
            while (maxFlow.obtainEPaths(vid, keepSet, pathMap).size() > 0) {

            }
        }

        // TODO: execute truss decomposition algorithm to get a k-truss containing the queryNode

        return keepSet;
    }
}
