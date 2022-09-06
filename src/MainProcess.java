import global.Constants;
import methods.GraphUtils;
import methods.LoadData;
import models.Edge.HeterogeneousEdge;
import models.Edge.HomogeneousEdge;
import models.graph.HeterogeneousGraph;
import models.graph.HomogeneousGraph;
import models.node.HeterogeneousNode;
import models.node.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * TODO: 结合nodeSplit完成点不相交
 * TODO: maxFlow不但能够返回最大流量，同时能返回同构图中相连的点 --- Done
 * TODO: 利用maxFlow返回的点构建重构图 --- Done
 * TODO: 对所有点都进行maxFlow算法 --- Done
 * TODO: createVirtualNode的时候需要把所有相同类型节点与virtualNode相连 --- Done
 * 关于图结构：
 * 多重图在每次使用最大流算法时需要重新构建，由于graphHashMap是new出来的对象，因此同时也重置了网络流量
 * virtualNode不需要使得nodeId成为特例
 * bfsTraverse用于筛选出无关的点
 * createMultipartGraph用于构建多重图
 * **/

public class MainProcess {
    public static void main(String[] args) throws IOException {
        List<HeterogeneousNode> nodeSet = new LinkedList<>();
        ArrayList<String[]> nodes = LoadData.loadNode();
        for (String[] node : nodes) {
            HeterogeneousNode heterogeneousNode = new HeterogeneousNode();
            heterogeneousNode.id = Integer.parseInt(node[0]);
            heterogeneousNode.nodeType = node[1];
            nodeSet.add(heterogeneousNode);
        }

        HeterogeneousGraph graph = new HeterogeneousGraph(nodeSet);

        ArrayList<int[]> edges = LoadData.loadEdge();
        for (int[] edge : edges) {
            HeterogeneousEdge heterogeneousEdge = new HeterogeneousEdge(Constants.SHARED_TIMES);
            heterogeneousEdge.setStartPoint(edge[0]);
            heterogeneousEdge.setEndPoint(edge[1]);
            graph.insertEdge(heterogeneousEdge);
        }
        int originNum = graph.vertexNum;

        // 筛选出和查询节点无关的点并删除从而节省存储空间
        List<HeterogeneousNode> inducedNodes = GraphUtils.bfsTraverse(graph, Constants.queryNodeId);

        // 生成导出子图，简化图结构
        graph.createInducedGraph(inducedNodes);
        int endPoint = originNum;

        int startPoint;

        HeterogeneousNode virtualSinkNode = new HeterogeneousNode();
        virtualSinkNode.id = endPoint;
        virtualSinkNode.nodeType = "sink";

        graph.addNode(virtualSinkNode);
        graph.vertexNum++;

        HomogeneousGraph homogeneousGraph = new HomogeneousGraph();
        for (HeterogeneousNode inducedNode : inducedNodes) {
            if (Objects.equals(inducedNode.nodeType, "A")) {
                startPoint = inducedNode.id;
                graph.createMultipartGraph(startPoint);
                // 根据锚点生成新的图结构
                graph.createVirtualSinkPath("A", endPoint);
                // 起始点与锚点连接的边的容量设置为零
                for (int j = 0; j < graph.graphHashMap.get(startPoint).size(); j++) {
                    if (graph.graphHashMap.get(startPoint).get(j).endPoint == endPoint) {
                        graph.graphHashMap.get(startPoint).get(j).capacity = 0;
                    }
                }

                // 对所有的点运用最大流算法，把异构图转变成同构图
                ArrayList<Node> homogeneousNodes = GraphUtils.maxFlow(graph, startPoint, endPoint);
                System.out.println("Done!");
                for (Node homogeneousNode : homogeneousNodes) {
                    HomogeneousEdge homogeneousEdge = new HomogeneousEdge();
                    homogeneousEdge.pointFirst = startPoint;
                    homogeneousEdge.pointSecond = homogeneousNode.id;
                    homogeneousGraph.insertEdge(homogeneousEdge);
                }
                homogeneousGraph.addNodes(homogeneousNodes);
            }
        }
        System.out.println("Done!");
    }
}
