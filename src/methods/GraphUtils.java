package methods;

import global.Constants;
import models.Edge.HeterogeneousEdge;
import models.graph.HeterogeneousGraph;
import models.node.HeterogeneousNode;

import java.util.*;

/**
 * TODO: 修改邻接链表的数据结构，把其中的元素从点转换为边
 * TODO: 修改邻接链表的存储结构，使其从原来的数组变成HashMap
 **/

public class GraphUtils {
    /**
     * 利用最大流找出异构图节点所有的元路径
     **/
    public static void maxFlow(HeterogeneousGraph graph, int startPoint, int endPoint) {
        ArrayList<Integer> parent = new ArrayList<Integer>();

        int u, v;
        int maxFlow = 0;
        int pathLength = 0;

        while (bfs(graph, startPoint, endPoint, parent)) {
            int pathFlow = Integer.MAX_VALUE;
            // 超过元路径长度的一半时和不超过元路径长度的一半时分别对待
            for (v = endPoint; v != startPoint; v = parent.get(v)) {
                u = parent.get(v);
                if (pathLength < (Constants.META_PATH_LENGTH - 1) / 2) {
                    int size = graph.hashMapReverse.get(u).size();
                    // 遍历邻接链表
                    for (int i = 0; i < size; i++) {
                        if (graph.hashMapReverse.get(u).get(i).getStartPoint() == v) {
                            pathFlow = Math.min(pathFlow, graph.hashMapReverse.get(u).get(i).capacity);
                        }
                    }
                } else {
                    int size = graph.hashMap.get(u).size();
                    // 遍历邻接链表
                    for (int i = 0; i < size; i++) {
                        if (graph.hashMap.get(u).get(i).getEndPoint() == v) {
                            pathFlow = Math.min(pathFlow, graph.hashMap.get(u).get(i).capacity);
                        }
                    }
                }

                pathLength++;
            }

            // 更新网络流量
            pathLength = 0;
            for (v = endPoint; v != startPoint; v = parent.get(v)) {
                u = parent.get(v);
                if (pathLength < (Constants.META_PATH_LENGTH - 1) / 2) {
                    int size = graph.hashMapReverse.get(u).size();
                    // 遍历邻接链表
                    for (int i = 0; i < size; i++) {
                        if (graph.hashMapReverse.get(u).get(i).getStartPoint() == v) {
                            graph.hashMapReverse.get(i).get(u).capacity -= pathFlow;
                            graph.hashMapReverse.get(u).get(i).capacity += pathFlow;
                        }
                    }
                } else {
                    int size = graph.hashMap.get(u).size();
                    // 遍历邻接链表
                    for (int i = 0; i < size; i++) {
                        if (graph.hashMap.get(u).get(i).getEndPoint() == v) {
                            graph.hashMap.get(u).get(i).capacity -= pathFlow;
                            graph.hashMap.get(i).get(u).capacity += pathFlow;
                        }
                    }
                }

                pathLength++;
            }

            maxFlow = pathFlow;
        }
        System.out.println(maxFlow);
    }

    /**
     * 广度优先搜索判断两点之间有无路径
     *
     * @return
     */
    public static boolean bfs(HeterogeneousGraph graph, int startPoint, int endPoint, ArrayList<Integer> parent) {
        ArrayList<Boolean> visited = new ArrayList<Boolean>();
        for (int i = 0; i < graph.vertexNum; i++) {
            // 全部初始化为false
            visited.add(false);
        }

        for (int i = 0; i < graph.vertexNum; i++) {
            parent.add(0);
        }
        parent.set(startPoint, -1);

        Queue<Integer> queue = new LinkedList<>();
        int currentPathLength = 0;
        int halfMetaPath = Constants.META_PATH_LENGTH / 2;

        queue.offer(startPoint);
        visited.set(startPoint, true);
        int u;
        // 初始化节点类型为空
        String tmpType = "";
        List<HeterogeneousNode> nodeSet = graph.getNodeSet();
        while (!queue.isEmpty()) {
            u = queue.poll();
            System.out.println(u);
            if (!tmpType.equals(nodeSet.get(u).nodeType)) {
                currentPathLength++;
                tmpType = nodeSet.get(u).nodeType;
            }
            // 未超过元路径长度的一半时
            if (currentPathLength <= halfMetaPath) {
                for (int i = 0; i < graph.hashMap.get(u).size(); i++) {
                    HeterogeneousEdge edge = graph.hashMap.get(u).get(i);
                    int point = graph.hashMap.get(u).get(i).getEndPoint();
                    judgeEqual(parent, visited, queue, currentPathLength, u, nodeSet, edge, point);
                }
            }
            // 超过元路径长度的一半时
            else {
                // 遍历到目标节点的时候会产生null值
                if (graph.hashMapReverse.get(u) != null) {
                    for (int i = 0; i < graph.hashMapReverse.get(u).size(); i++) {
                        HeterogeneousEdge edge = graph.hashMap.get(u).get(i);
                        int point = graph.hashMapReverse.get(u).get(i).getStartPoint();
                        judgeEqual(parent, visited, queue, currentPathLength, u, nodeSet, edge, point);
                    }
                }
            }
        }
        return visited.get(endPoint);
    }

    private static void judgeEqual(
            ArrayList<Integer> parent,
            ArrayList<Boolean> visited,
            Queue<Integer> queue,
            int currentPathLength,
            int u, List<HeterogeneousNode> nodeSet,
            HeterogeneousEdge edge,
            int point
    ) {
        if ((Objects.equals(nodeSet.get(u).nodeType, Constants.META_PATH[currentPathLength])
                || Objects.equals(nodeSet.get(u).nodeType, "virtual"))
                && !visited.get(point)
                && edge.capacity > 0
        ) {
            queue.offer(point);
            visited.set(point, true);
            parent.set(point, u);
        }
    }

    /**
     * 广度优先遍历初步筛选异构图，剔除无法通过元路径与查询节点相连的点
     **/
    public static List<HeterogeneousNode> bfsTraverse(HeterogeneousGraph graph, int queryNodeId) {
        List<HeterogeneousNode> nodes = new ArrayList<>();

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
        // 初始化节点类型为空
        String tmpType = "";
        List<HeterogeneousNode> nodeSet = graph.getNodeSet();
        while (!queue.isEmpty()) {
            u = queue.poll();
            // 存储与查询节点有关的点
            System.out.println(u);
            String nodeType = "";
            for (HeterogeneousNode heterogeneousNode : nodeSet) {
                if (heterogeneousNode.id == u) {
                    nodeType = heterogeneousNode.nodeType;
                    nodes.add(heterogeneousNode);
                }
            }
            if (!tmpType.equals(nodeType)) {
                currentPathLength++;
                tmpType = nodeSet.get(u).nodeType;
            }
            // 未超过元路径长度的一半时
            if (currentPathLength <= halfMetaPath) {
                for (int i = 0; i < graph.hashMap.get(u).size(); i++) {
                    int point = ((HeterogeneousEdge) graph.hashMap.get(u).get(i)).getEndPoint();
                    if (Objects.equals(nodeSet.get(u).nodeType, Constants.META_PATH[currentPathLength]) && !visited.get(point)
                            || Objects.equals(nodeSet.get(u).nodeType, "virtual")) {
                        queue.offer(point);
                        visited.set(point, true);
                    }
                }
            }
            // 超过元路径长度的一半时
            else {
                // 遍历到目标节点的时候会产生null值
                if (graph.hashMapReverse.get(u) != null) {
                    for (int i = 0; i < graph.hashMapReverse.get(u).size(); i++) {
                        int point = ((HeterogeneousEdge) graph.hashMapReverse.get(u).get(i)).getStartPoint();
                        if (Objects.equals(nodeSet.get(u).nodeType, Constants.META_PATH[currentPathLength]) && !visited.get(point)
                                || Objects.equals(nodeSet.get(u).nodeType, "virtual")) {
                            queue.offer(point);
                            visited.set(point, true);
                        }
                    }
                }
            }
        }
        return nodes;
    }
}
