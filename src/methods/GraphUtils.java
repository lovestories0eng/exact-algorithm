package methods;

import global.Constants;
import models.graph.HeterogeneousGraph;
import models.node.HeterogeneousNode;

import java.util.*;

public class GraphUtils {
    /**
     利用最大流找出异构图节点所有的元路径
    **/
    public void maxFlow(int nodeId) {

    }

    /**
     * 广度优先遍历初步筛选异构图，剔除无法通过元路径与查询节点相连的点
    **/
    public static void bfs(HeterogeneousGraph graph, int queryNodeId) {
        ArrayList<Boolean> visited = new ArrayList<Boolean>();
        for (int i = 0; i < graph.vertexNum; i++) {
            // 全部初始化为false
            visited.add(false);
        }
        Queue<Integer> queue = new LinkedList<>();
        int currentPathLength = 0;
        int halfMetaPath = Constants.META_PATH_LENGTH / 2;

        queue.offer(queryNodeId);
        visited.set(queryNodeId, true);
        int u;
        String tmpType = "";
        List<HeterogeneousNode> nodeSet = graph.getNodeSet();
        while (!queue.isEmpty()) {
            u = queue.poll();
            System.out.println(u);
            if (!tmpType.equals(nodeSet.get(u).nodeType)) {
                currentPathLength++;
                tmpType = nodeSet.get(u).nodeType;
                // 如果当前路径的长度已经大于了元路径的长度，跳出循环
                if (currentPathLength >= Constants.META_PATH_LENGTH + 1)
                    break;
            }

            // 未超过元路径长度的一半时
            if (currentPathLength <= halfMetaPath) {
                for (int i = 0; i < graph.adjacencyLinkedList[u].size();i++) {
                    int point = (int) graph.adjacencyLinkedList[u].get(i);
                    if (Objects.equals(nodeSet.get(u).nodeType, Constants.META_PATH[currentPathLength]) && !visited.get(point)
                            || Objects.equals(nodeSet.get(u).nodeType, "virtual")) {
                        queue.offer(point);
                        visited.set(point, true);
                    }
                }
            }
            // 超过元路径长度的一半时
            else {
                for (int i = 0; i < graph.adjacencyLinkedListReverse[u].size();i++) {
                    int point = (int) graph.adjacencyLinkedListReverse[u].get(i);
                    if (Objects.equals(nodeSet.get(u).nodeType, Constants.META_PATH[currentPathLength]) && !visited.get(point)
                            || Objects.equals(nodeSet.get(u).nodeType, "virtual")) {
                        queue.offer(point);
                        visited.set(point, true);
                    }
                }
            }
        }
    }
}
