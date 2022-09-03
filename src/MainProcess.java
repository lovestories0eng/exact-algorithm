import global.Constants;
import methods.GraphUtils;
import methods.LoadData;
import models.Edge.HeterogeneousEdge;
import models.graph.HeterogeneousGraph;
import models.graph.HomogeneousGraph;
import models.node.HeterogeneousNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
        System.out.println(graph.vertexNum);

        // 筛选出和查询节点无关的点并删除从而节省存储空间
        List<HeterogeneousNode> inducedNodes = GraphUtils.bfsTraverse(graph, Constants.queryNodeId);

        // 生成导出子图，简化图结构
        graph.createInducedGraph(inducedNodes);

        // 根据锚点生成新的图结构
        graph.createVirtualSinkNode(0, originNum);

        System.out.println("s");

        // 对所有的点运用最大流算法，把异构图转变成同构图
        HomogeneousGraph homogeneousGraph = new HomogeneousGraph();
        GraphUtils.maxFlow(graph, 0, originNum);
    }
}
