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
    public void maxFlow(HeterogeneousGraph graph, int startPoint, int endPoint) {
        ArrayList<Integer> parent = new ArrayList<Integer>();
        for (int i = 0; i < graph.vertexNum; i++) {
            parent.set(i, 0);
        }
        parent.set(startPoint, -1);

        int u, v;
        int maxFlow = 0;
        int pathLength = 0;

        while (bfs(graph, startPoint, endPoint, parent)) {
            int pathFlow = Integer.MAX_VALUE;
            for (v = endPoint; v != startPoint; v = parent.get(v)) {
                u = parent.get(v);
                pathFlow = Math.min(pathFlow, graph.hashMapReverse.get(u).get(v).capacity);
                pathLength++;
            }
            for (v = endPoint; v != startPoint; v = parent.get(v)) {
                u = parent.get(v);
                graph.hashMapReverse.get(u).get(v).capacity -= pathFlow;
                graph.hashMapReverse.get(v).get(u).capacity += pathFlow;
                pathLength++;
            }

            maxFlow = pathFlow;
        }
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
    }

    /**
     * 广度优先搜索判断两点之间有无路径
     *
     * @return
     */
    public boolean bfs(HeterogeneousGraph graph, int startPoint, int endPoint, ArrayList<Integer> parent) {
        ArrayList<Boolean> visited = new ArrayList<Boolean>();
        for (int i = 0; i < graph.vertexNum; i++) {
            // 全部初始化为false
            visited.add(false);
        }

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
                // 遍历到目标节点的时候会产生null值
                if (graph.hashMapReverse.get(u) != null) {
                    for (int i = 0; i < graph.hashMapReverse.get(u).size(); i++) {
                        HeterogeneousEdge edge = graph.hashMap.get(u).get(i);
                        int point = graph.hashMapReverse.get(u).get(i).getStartPoint();
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
        }
        return visited.get(endPoint);
    }
}
