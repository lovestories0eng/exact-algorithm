package utils.impl;

import global.Config;

import java.util.*;

public class TrussDecomposition {
    private final HashMap<Integer, Set<Integer>> homoGraph;
    private HashMap<Integer, Set<Integer>> homoGraphDeepCopy;
    // 使用点对来代表一条边，小的id在左，大的id在右
    Set<Map.Entry<Integer, Integer>> edges;
    // 记录所有边支持度
    HashMap<Map.Entry<Integer, Integer>, Integer> support;
    // 记录所有边的trussness值
    HashMap<Map.Entry<Integer, Integer>, Integer> edgeTruss;
    // 边映射到Id
    HashMap<Integer, Map.Entry<Integer, Integer>> idMapEdge;
    // id映射到边
    HashMap<Map.Entry<Integer, Integer>, Integer> edgeMapId;
    HashMap<Integer, Integer> trussChecker;
    // 数组，值代表边的id
    List<Integer> edgesId;
    int k;

    public TrussDecomposition(HashMap<Integer, Set<Integer>> homoGraph, int k) {
        this.homoGraph = homoGraph;
        // HashSet不含有相同元素
        this.edges = new HashSet<>();
        this.k = k;
        for (Map.Entry<Integer, Set<Integer>> entry : homoGraph.entrySet()) {
            int vertex = entry.getKey();
            Set<Integer> adjacent = entry.getValue();
            for (int adj : adjacent) {
                if (adj < vertex) {
                    edges.add(Map.entry(adj, vertex));
                } else if (adj > vertex) {
                    edges.add(Map.entry(vertex, adj));
                }
            }
        }
    }

    public Set<Integer> executeDecompose() {
        initSupport();

        System.out.println(homoGraph.get(Config.queryNodeId).size());
        System.out.println("Break point!");

        Iterator<Map.Entry<Integer, Integer>> it = support.keySet().iterator();

        // 剪枝，删除支持度小于 k - 2 的边
        // TODO:同时更新受影响的边
        // while (it.hasNext()) {
        //     Map.Entry<Integer, Integer> tmpEdge = it.next();
        //     int sup = support.get(tmpEdge);
        //     if (sup < Config.k - 2) {
        //         it.remove();
        //         int u = tmpEdge.getKey();
        //         int v = tmpEdge.getValue();
        //         homoGraph.get(u).remove(v);
        //         homoGraph.get(v).remove(u);
        //         edges.remove(tmpEdge);
        //         support.remove(tmpEdge);
        //     }
        // }

        this.deepCopyHomoGraph();

        System.out.println(homoGraphDeepCopy.get(Config.queryNodeId).size());
        System.out.println("Break point!");

        mergeSort();
        for (int i = 0; i < edgesId.size() - 1; i++) {
            System.out.println(support.get(idMapEdge.get(edgesId.get(i))));
        }
        int tmpK = 2;
        while (edgesId.size() > 0) {
            while (support.get(idMapEdge.get(edgesId.get(0))) <= tmpK - 2) {
                // Let e = (u, v) be the edge with the lowest support;
                Map.Entry<Integer, Integer> tmpEdge = idMapEdge.get(edgesId.get(0));
                int u = tmpEdge.getKey();
                int v = tmpEdge.getValue();
                // 点u的度数
                int du = homoGraph.get(u).size();
                // 点v的度数
                int dv = homoGraph.get(v).size();
                // 度数小的点的邻居集合
                int nu;
                boolean flag;
                // Assume, w.l.o.g, d(u) ≤ d(v);
                if (du <= dv) {
                    // u
                    nu = tmpEdge.getKey();
                    flag = false;
                } else {
                    // v
                    nu = tmpEdge.getValue();
                    flag = true;
                }
                int x, y;
                for (int w : homoGraph.get(nu)) {
                    if (!flag) {
                        if (homoGraph.get(w).contains(v) && w != v) {
                            x = updateSupport(u, w);
                            y = updateSupport(v, w);
                            // Reorder (u, w) and (v, w) w.r.t. new edge support;
                            int xIndex = edgesId.indexOf(x);
                            int yIndex = edgesId.indexOf(y);
                            int tmp = edgesId.get(xIndex);
                            edgesId.set(xIndex, edgesId.get(yIndex));
                            edgesId.set(yIndex, tmp);
                        }
                    } else {
                        if (homoGraph.get(w).contains(u) && w != u) {
                            x = updateSupport(u, w);
                            y = updateSupport(v, w);
                            // Reorder (u, w) and (v, w) w.r.t. new edge support;
                            int xIndex = edgesId.indexOf(x);
                            int yIndex = edgesId.indexOf(y);
                            int tmp = edgesId.get(xIndex);
                            edgesId.set(xIndex, edgesId.get(yIndex));
                            edgesId.set(yIndex, tmp);
                        }
                    }
                }
                // τ(e∗) ← k, remove e∗ from EG;
                edgeTruss.put(tmpEdge, tmpK);
                // 删除支持度最小的边
                homoGraph.get(u).remove(v);
                homoGraph.get(v).remove(u);
                edges.remove(tmpEdge);
                idMapEdge.remove(edgesId.get(0));
                edgeMapId.remove(tmpEdge);
                edgesId.remove(0);
                if (edgesId.size() == 0) break;
            }
            tmpK++;
        }



        this.trussChecker();

        Set<Integer> result = findKTruss();
        return result;
    }

    private void trussChecker() {
        trussChecker = new HashMap<>();
        for (Map.Entry<Map.Entry<Integer, Integer>, Integer> entry : edgeTruss.entrySet()) {
            int value = entry.getValue();
            if (trussChecker.containsKey(value)) {
                trussChecker.put(value, trussChecker.get(value) + 1);
            } else {
                trussChecker.put(value, 1);
            }
        }
    }

    // 深拷贝homoGraph
    private void deepCopyHomoGraph() {
        homoGraphDeepCopy = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> integerSetEntry : homoGraph.entrySet()) {
            Set<Integer> tmpSet = new HashSet<>(integerSetEntry.getValue());
            homoGraphDeepCopy.put(integerSetEntry.getKey(), tmpSet);
        }
    }

    private Set<Integer> findKTruss() {
        // 去除无查询节点的连通分量
        int queryNode = Config.queryNodeId;
        Set<Integer> visitSet = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(queryNode);
        visitSet.add(queryNode);

        while (!queue.isEmpty()) {
            int out = queue.poll();
            Set<Integer> tmpInt = homoGraphDeepCopy.get(out);
            System.out.println("Break point!");
            for (Integer adj : homoGraphDeepCopy.get(out)) {
                Map.Entry<Integer, Integer> tmpEdge;
                if (out < adj) {
                    tmpEdge = Map.entry(out, adj);
                }
                else {
                    tmpEdge = Map.entry(adj, out);
                }
                if (!visitSet.contains(adj) && edgeTruss.get(tmpEdge) != null && edgeTruss.get(tmpEdge) >= Config.k) {
                    visitSet.add(adj);
                    queue.offer(adj);
                }
            }
        }
        return visitSet;
    }

    private int updateSupport(int v, int w) {
        int sup;
        if (v < w) {
            sup = support.get(Map.entry(v, w));
            support.put(Map.entry(v, w), sup - 1);
            return edgeMapId.get(Map.entry(v, w));
        } else {
            sup = support.get(Map.entry(w, v));
            support.put(Map.entry(w, v), sup - 1);
            return edgeMapId.get(Map.entry(w, v));
        }
    }

    private void initSupport() {
        support = new HashMap<>();
        idMapEdge = new HashMap<>();
        edgeMapId = new HashMap<>();
        edgesId = new ArrayList<>();
        edgeTruss = new HashMap<>();
        int count = 0;
        for (Map.Entry<Integer, Integer> edge : edges) {
            int startPoint = edge.getKey();
            int endPoint = edge.getValue();
            Set<Integer> startPointAdj = homoGraph.get(startPoint);
            Set<Integer> endPointAdj = homoGraph.get(endPoint);
            Set<Integer> tmp = new HashSet<>(startPointAdj);
            tmp.retainAll(endPointAdj);
            support.put(edge, tmp.size());
            idMapEdge.put(count, edge);
            edgeMapId.put(edge, count);
            count++;
        }

        Iterator<Integer> it = idMapEdge.keySet().iterator();
        while (it.hasNext()) {
            int id = it.next();
            Map.Entry<Integer, Integer> tmpEdge = idMapEdge.get(id);
            // 只把支持度大于等于k的边加进去
            // if (support.get(tmpEdge) >= Config.k - 2) {
                edgesId.add(id);
            // } else {
            //     edgeMapId.remove(tmpEdge);
            //     it.remove();
            // }
        }
        System.out.println("Break point!");
    }

    public void mergeSort() {
        int[] temp = new int[edgesId.size()];
        int gap = 1;

        while (gap < edgesId.size()) {
            for (int i = 0; i < edgesId.size(); i += gap * 2) {
                int mid = i + gap;
                int right = mid + gap;

                if (mid > edgesId.size()) {
                    mid = edgesId.size();
                }
                if (right > edgesId.size()) {
                    right = edgesId.size();
                }
                mergeData(i, mid, right, temp);
            }
            for (int i = 0; i < edgesId.size(); i++) {
                edgesId.set(i, temp[i]);
            }
            // gap *= 2;
            gap <<= 1;
        }
    }

    // 合并数据  [left,mid)  [mid,right)
    private void mergeData(int left, int mid, int right, int[] temp) {
        int index = left;
        int begin1 = left, begin2 = mid;

        while (begin1 < mid && begin2 < right) {
            if (support.get(idMapEdge.get(edgesId.get(begin1))) <= support.get(idMapEdge.get(edgesId.get(begin2)))) {
                temp[index++] = edgesId.get(begin1++);
            } else {
                temp[index++] = edgesId.get(begin2++);
            }
        }
        // 如果第一个区间中还有数据
        while (begin1 < mid) {
            temp[index++] = edgesId.get(begin1++);
        }
        // 如果第二个区间有数据
        while (begin2 < right) {
            temp[index++] = edgesId.get(begin2++);
        }
    }
}
