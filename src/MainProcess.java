import global.Constants;
import methods.GraphUtils;
import methods.LoadData;
import models.Edge.HeterogeneousEdge;
import models.graph.HeterogeneousGraph;
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

        HeterogeneousGraph graph = new HeterogeneousGraph(nodeSet.size(), nodeSet);

        ArrayList<int[]> edges = LoadData.loadEdge();
        for (int[] edge : edges) {
            HeterogeneousEdge heterogeneousEdge = new HeterogeneousEdge(Constants.SHARED_TIMES);
            heterogeneousEdge.setStartPoint(edge[0]);
            heterogeneousEdge.setEndPoint(edge[1]);
            graph.insertEdge(heterogeneousEdge);
        }

        GraphUtils.bfs(graph, 0);
    }
}
