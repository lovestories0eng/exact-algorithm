package models.node;

public class HeterogeneousNode extends Node {
    // 节点类型（异构图）
    public String nodeType;
    /**
     * 在网络最大流算法时需要寻找新节点，
     * 用于判断此节点是顺着元路径还是逆着元路径
     * **/
    public boolean direction;
    // 节点在元路径中的位置(一个节点可能位于元路径的两个位置，因为元路径是对称的)
    public int position;
}
