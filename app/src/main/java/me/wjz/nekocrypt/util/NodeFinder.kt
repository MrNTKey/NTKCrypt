package me.wjz.nekocrypt.util

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import me.wjz.nekocrypt.NekoCryptApp

/**
 * ✨ 优雅的无障碍节点查找工具类
 * 提供多种灵活的节点查找策略，支持条件组合查找
 * 
 * @author 猫娘老师 🐱
 */
object NodeFinder {
    private const val TAG = NekoCryptApp.TAG

    /**
     * ✨ 优雅的节点查找方法 - 支持多种查找条件的组合
     * @param rootNode 根节点，查找的起点
     * @param viewId 视图ID，可为null
     * @param className 类名（支持部分匹配），可为null  
     * @param text 节点文本内容，可为null
     * @param contentDescription 内容描述，可为null
     * @param predicate 自定义谓词条件，可为null
     * @param findAll 是否查找所有匹配的节点，默认false（只返回第一个）
     * @return 如果findAll=false，返回第一个匹配节点；如果findAll=true，返回所有匹配节点的列表
     */
    fun findNodeByConditions(
        rootNode: AccessibilityNodeInfo,
        viewId: String? = null,
        className: String? = null,
        text: String? = null,
        contentDescription: String? = null,
        predicate: ((AccessibilityNodeInfo) -> Boolean)? = null,
        findAll: Boolean = false
    ): Any? {
        
        return if (findAll) {
            // 查找所有匹配的节点
            findAllNodesByConditions(rootNode, viewId, className, text, contentDescription, predicate)
        } else {
            // 查找第一个匹配的节点
            findSingleNodeByConditions(rootNode, viewId, className, text, contentDescription, predicate)
        }
    }

    /**
     * 🎯 查找第一个匹配条件的节点
     */
    private fun findSingleNodeByConditions(
        rootNode: AccessibilityNodeInfo,
        viewId: String?,
        className: String?,
        text: String?,
        contentDescription: String?,
        predicate: ((AccessibilityNodeInfo) -> Boolean)?
    ): AccessibilityNodeInfo? {
        
        // 🎯 策略1: 如果提供了viewId，优先精确查找
        viewId?.takeIf { it.isNotEmpty() }?.let { id ->
            val candidates = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (!candidates.isNullOrEmpty()) {
                // 在ID匹配的候选者中进一步筛选
                return candidates.firstOrNull { node ->
                    matchesAllConditions(node, className, text, contentDescription, predicate)
                }?.also {
                    Log.d(TAG, "✅ 通过viewId找到节点: $id")
                }
            }
        }
        
        // 🎯 策略2: 递归遍历查找（当没有viewId或ID查找失败时）
        return findNodeRecursively(rootNode, className, text, contentDescription, predicate)
            ?.also { Log.d(TAG, "✅ 通过递归查找找到节点") }
            ?: run {
                Log.d(TAG, "❌ 未找到匹配条件的节点 [viewId=$viewId, className=$className]")
                null
            }
    }

    /**
     * 🔍 查找所有匹配条件的节点
     * @return 匹配的节点列表，可能为空
     */
    fun findAllNodesByConditions(
        rootNode: AccessibilityNodeInfo,
        viewId: String? = null,
        className: String? = null,
        text: String? = null,
        contentDescription: String? = null,
        predicate: ((AccessibilityNodeInfo) -> Boolean)? = null
    ): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        
        // 策略1: 如果提供了viewId，优先精确查找
        viewId?.takeIf { it.isNotEmpty() }?.let { id ->
            val candidates = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (!candidates.isNullOrEmpty()) {
                candidates.filter { node ->
                    matchesAllConditions(node, className, text, contentDescription, predicate)
                }.let { results.addAll(it) }
            }
        }
        
        // 策略2: 递归查找（如果没有通过ID找到或者没有提供ID）
        if (results.isEmpty() || viewId.isNullOrEmpty()) {
            findAllNodesRecursively(rootNode, className, text, contentDescription, predicate, results)
        }
        
        Log.d(TAG, "找到 ${results.size} 个匹配的节点")
        return results
    }

    /**
     * 🎯 查找最大的可滚动容器（通常是消息列表）
     */
    fun findLargestScrollableContainer(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var largestScrollable: AccessibilityNodeInfo? = null
        var maxChildCount = 0
        
        fun searchScrollable(node: AccessibilityNodeInfo) {
            if (node.isScrollable && node.childCount > maxChildCount) {
                largestScrollable = node
                maxChildCount = node.childCount
            }
            
            repeat(node.childCount) { i ->
                node.getChild(i)?.let { child ->
                    searchScrollable(child)
                }
            }
        }
        
        searchScrollable(rootNode)
        return largestScrollable?.also {
            Log.d(TAG, "✅ 找到最大可滚动容器，子节点数: $maxChildCount")
        }
    }

    /**
     * 🔍 查找所有包含指定文本的节点
     */
    fun findNodesByText(
        rootNode: AccessibilityNodeInfo,
        targetText: String,
        exactMatch: Boolean = false
    ): List<AccessibilityNodeInfo> {
        return findAllNodesByConditions(
            rootNode = rootNode,
            predicate = { node ->
                val nodeText = node.text?.toString()
                when {
                    nodeText.isNullOrEmpty() -> false
                    exactMatch -> nodeText == targetText
                    else -> nodeText.contains(targetText, ignoreCase = true)
                }
            }
        )
    }

    /**
     * 🎯 查找可点击的按钮节点
     */
    fun findClickableButtons(
        rootNode: AccessibilityNodeInfo,
        buttonText: String? = null
    ): List<AccessibilityNodeInfo> {
        return findAllNodesByConditions(
            rootNode = rootNode,
            className = "Button",
            text = buttonText,
            predicate = { it.isClickable && it.isEnabled }
        )
    }

    /**
     * 🔍 查找输入框节点
     */
    fun findEditTextNodes(rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        return findAllNodesByConditions(
            rootNode = rootNode,
            className = "EditText",
            predicate = { it.isEditable }
        )
    }

    /**
     * 🎯 验证节点是否仍然有效
     */
    fun isNodeValid(node: AccessibilityNodeInfo?): Boolean {
        return node?.refresh() ?: false
    }

    /**
     * 🔍 递归查找节点的核心逻辑
     */
    private fun findNodeRecursively(
        node: AccessibilityNodeInfo,
        className: String?,
        text: String?,
        contentDescription: String?,
        predicate: ((AccessibilityNodeInfo) -> Boolean)?
    ): AccessibilityNodeInfo? {
        
        // 检查当前节点是否匹配所有条件
        if (matchesAllConditions(node, className, text, contentDescription, predicate)) {
            return node
        }
        
        // 递归检查子节点
        repeat(node.childCount) { i ->
            node.getChild(i)?.let { child ->
                findNodeRecursively(child, className, text, contentDescription, predicate)
                    ?.let { return it }
            }
        }
        
        return null
    }

    /**
     * 🔍 递归查找所有匹配的节点
     */
    private fun findAllNodesRecursively(
        node: AccessibilityNodeInfo,
        className: String?,
        text: String?,
        contentDescription: String?,
        predicate: ((AccessibilityNodeInfo) -> Boolean)?,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        // 检查当前节点是否匹配所有条件
        if (matchesAllConditions(node, className, text, contentDescription, predicate)) {
            results.add(node)
        }
        
        // 递归检查子节点
        repeat(node.childCount) { i ->
            node.getChild(i)?.let { child ->
                findAllNodesRecursively(child, className, text, contentDescription, predicate, results)
            }
        }
    }

    /**
     * 🎯 检查节点是否匹配所有给定条件
     */
    private fun matchesAllConditions(
        node: AccessibilityNodeInfo,
        className: String?,
        text: String?,
        contentDescription: String?,
        predicate: ((AccessibilityNodeInfo) -> Boolean)?
    ): Boolean {
        return listOfNotNull(
            // className 条件检查
            className?.let { 
                node.className?.toString()?.contains(it, ignoreCase = true) == true 
            },
            // text 条件检查  
            text?.let {
                node.text?.toString()?.contains(it, ignoreCase = true) == true
            },
            // contentDescription 条件检查
            contentDescription?.let {
                node.contentDescription?.toString()?.contains(it, ignoreCase = true) == true
            },
            // 自定义谓词条件检查
            predicate?.let { it(node) }
        ).all { it } // 所有非null条件都必须为true
    }

    /**
     * 🐾 调试用：打印节点树结构
     */
    fun debugNodeTree(
        node: AccessibilityNodeInfo?,
        maxDepth: Int = 5,
        currentDepth: Int = 0
    ) {
        if (node == null || currentDepth > maxDepth) return
        
        val indent = "  ".repeat(currentDepth)
        val className = node.className?.toString() ?: "null"
        val text = node.text?.toString()?.take(20) ?: ""
        val desc = node.contentDescription?.toString()?.take(20) ?: ""
        
        Log.d(TAG, "$indent[$currentDepth] $className")
        Log.d(TAG, "$indent    文本: '$text'")
        Log.d(TAG, "$indent    描述: '$desc'")
        Log.d(TAG, "$indent    ID: ${node.viewIdResourceName}")
        Log.d(TAG, "$indent    属性: [可点击:${node.isClickable}, 可滚动:${node.isScrollable}, 可编辑:${node.isEditable}]")
        
        repeat(node.childCount) { i ->
            node.getChild(i)?.let { child ->
                debugNodeTree(child, maxDepth, currentDepth + 1)
            }
        }
    }

    // ✨ 便捷扩展方法，让使用更加优雅
    
    /**
     * 🎯 查找单个节点的便捷方法
     */
    fun findSingleNode(
        rootNode: AccessibilityNodeInfo,
        viewId: String? = null,
        className: String? = null,
        text: String? = null,
        contentDescription: String? = null,
        predicate: ((AccessibilityNodeInfo) -> Boolean)? = null
    ): AccessibilityNodeInfo? {
        return findNodeByConditions(rootNode, viewId, className, text, contentDescription, predicate, false) as? AccessibilityNodeInfo
    }

    /**
     * 🎯 查找多个节点的便捷方法
     */
    fun findMultipleNodes(
        rootNode: AccessibilityNodeInfo,
        viewId: String? = null,
        className: String? = null,
        text: String? = null,
        contentDescription: String? = null,
        predicate: ((AccessibilityNodeInfo) -> Boolean)? = null
    ): List<AccessibilityNodeInfo> {
        return findNodeByConditions(rootNode, viewId, className, text, contentDescription, predicate, true) as? List<AccessibilityNodeInfo> ?: emptyList()
    }
}
