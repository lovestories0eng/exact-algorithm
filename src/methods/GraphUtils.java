package methods;

import global.Constants;
import models.Edge.HeterogeneousEdge;
import models.graph.HeterogeneousGraph;
import models.node.HeterogeneousNode;

import java.util.*;

/**
 * TODO: 修改邻接链表的数据结构，把其中的元素从点转换为边
 * TODO: 修改邻接链表的存储结构，使其从原来的数组变成HashMap
 * **/

public class GraphUtils {
    /**
     利用最大流找出异构图节点所有的元路径
    **/
    public void maxFlow(int nodeId) {

    }

    /**
     * 广度优先遍历初步筛选异构图，剔除无法通过元路径与查询节点相连的点
    **/
    public static void bfsTraverse(HeterogeneousGraph graph, int queryNodeId) {
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
                    int point = ((HeterogeneousEdge) graph.adjacencyLinkedList[u].get(i)).getEndPoint();
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
                    int point = ((HeterogeneousEdge) graph.adjacencyLinkedListReverse[u].get(i)).getStartPoint();
                    if (Objects.equals(nodeSet.get(u).nodeType, Constants.META_PATH[currentPathLength]) && !visited.get(point)
                            || Objects.equals(nodeSet.get(u).nodeType, "virtual")) {
                        queue.offer(point);
                        visited.set(point, true);
                    }
                }
            }
        }
    }

    /**
     * 广度优先搜索判断两点之间有无路径
     *
     * @return*/
    public boolean bfs(HeterogeneousGraph graph, int startPoint, int endPoint) {
        ArrayList<Boolean> visited = new ArrayList<Boolean>();
        for (int i = 0; i < graph.vertexNum; i++) {
            // 全部初始化为false
            visited.add(false);
        }
        ArrayList<Integer> parent = new ArrayList<>();
        for (int i = 0; i< graph.vertexNum; i++) {
            parent.add(0);
        }
        // 起始点的前置节点设置为-1
        parent.set(startPoint, -1);

        Queue<Integer> queue = new LinkedList<>();
        int currentPathLength = 0;
        int halfMetaPath = Constants.META_PATH_LENGTH / 2;

        queue.offer(startPoint);
        visited.set(startPoint, true);
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
                    HeterogeneousEdge edge = (HeterogeneousEdge) graph.adjacencyLinkedList[u].get(i);
                    int point = edge.getEndPoint();
                    if ((Objects.equals(nodeSet.get(u).nodeType, Constants.META_PATH[currentPathLength])
                            || Objects.equals(nodeSet.get(u).nodeType, "virtual"))
                            && !visited.get(point)
                            && edge.capacity > 0
                            ) {
                        queue.offer(point);
                        visited.set(point, true);
                    }
                }
            }
            // 超过元路径长度的一半时
            else {
                for (int i = 0; i < graph.adjacencyLinkedListReverse[u].size();i++) {
                    HeterogeneousEdge edge = (HeterogeneousEdge) graph.adjacencyLinkedListReverse[u].get(i);
                    int point = edge.getStartPoint();
                    if ((Objects.equals(nodeSet.get(u).nodeType, Constants.META_PATH[currentPathLength])
                            || Objects.equals(nodeSet.get(u).nodeType, "virtual"))
                            && !visited.get(point)
                            && edge.capacity > 0
                            ) {
                        queue.offer(point);
                        visited.set(point, true);
                    }
                }
            }
        }
        return visited.get(endPoint);
    }
}
