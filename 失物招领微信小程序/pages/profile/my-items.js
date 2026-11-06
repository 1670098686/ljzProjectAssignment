// pages/profile/my-items.js
const { get } = require('../../utils/request');
const { requireLogin } = require('../../utils/auth');
const { showToast, formatRelativeTime, getBizStatusText } = require('../../utils/util');

Page({
  data: {
    status: 'all',
    currentStatus: 'all',
    pageTitle: '我的发布',
    allItems: [],
    filteredItems: [],
    
    page: 1,
    pageSize: 5,
    total: 0,
    totalPages: 0,
    hasMore: true,
    isLoading: false,
    
    loading: true,
    loadingMore: false,
    refreshing: false,
    noMoreData: false
  },

  onLoad(options) {
    requireLogin(() => {
      const status = options.status || 'all';
      this.setData({
        status: status,
        currentStatus: status,
        pageTitle: this.getStatusTitle(status)
      });
      this.loadFirstPage();
    });
  },

  onShow() {
    // 每次显示页面时都刷新数据，确保实时性
    this.loadFirstPage();
  },

  getStatusTitle(status) {
    const titles = {
      'all': '我的发布',
      'PENDING': '待发布',
      'APPROVED': '进行中',
      'REJECTED': '已驳回',
      'CANCELLED': '已取消',
      'RESOLVED': '已找回',
      'RETURNED': '已归还'
    };
    return titles[status] || '我的发布';
  },

  loadFirstPage() {
    this.setData({
      page: 1,
      allItems: [],
      filteredItems: [],
      hasMore: true,
      loading: true,
      noMoreData: false
    });
    this.loadListData(1, true);
  },

  loadNextPage() {
    if (this.data.isLoading || !this.data.hasMore) {
      return;
    }
    
    const nextPage = this.data.page + 1;
    this.setData({
      loadingMore: true,
      isLoading: true
    });
    
    this.loadListData(nextPage, false);
  },

  loadListData(page, isFirstLoad) {
    const that = this;
    get('/auth/me/items', {
      page: page,
      pageSize: this.data.pageSize
    }).then(res => {
      const list = res.data.list || [];
      const total = res.data.total;
      const totalPages = res.data.totalPage;
      
      const newItems = [];
      for (let i = 0; i < list.length; i++) {
        const item = list[i];
        newItems.push({
          id: item.id,
          title: item.title,
          itemType: item.itemType,
          auditStatus: item.auditStatus,
          bizStatus: item.bizStatus,
          location: item.location,
          happenTime: item.happenTime,
          publishedAt: item.publishedAt,
          publishedAtText: formatRelativeTime(item.publishedAt),
          bizStatusText: getBizStatusText(item.bizStatus, item.itemType, item.auditStatus)
        });
      }
      
      let allItems = [];
      if (isFirstLoad) {
        allItems = newItems;
      } else {
        allItems = that.data.allItems.concat(newItems);
      }
      
      const hasMore = page < totalPages;
      
      that.setData({
        page: page,
        total: total,
        totalPages: totalPages,
        allItems: allItems,
        filteredItems: that.filterItems(allItems, that.data.status),
        hasMore: hasMore,
        noMoreData: !hasMore,
        loading: false,
        loadingMore: false,
        isLoading: false,
        refreshing: false
      });
      
      if (isFirstLoad) {
        wx.stopPullDownRefresh();
      }
      
    }).catch(err => {
      console.error('加载我的发布失败:', err);
      showToast('加载失败');
      that.setData({
        loading: false,
        loadingMore: false,
        isLoading: false,
        refreshing: false
      });
      
      if (isFirstLoad) {
        wx.stopPullDownRefresh();
      }
    });
  },

  filterItems(items, status) {
    if (status === 'all') return items;
    const filtered = [];
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      switch (status) {
        case 'PENDING':
          // 待审核 -> 待发布（审核状态PENDING + 业务状态ONGOING）
          if (item.auditStatus === 'PENDING' && item.bizStatus === 'ONGOING') {
            filtered.push(item);
          }
          break;
        case 'APPROVED':
          // 已通过 -> 进行中（审核状态APPROVED + 业务状态ONGOING）
          if (item.auditStatus === 'APPROVED' && item.bizStatus === 'ONGOING') {
            filtered.push(item);
          }
          break;
        case 'REJECTED':
          // 已驳回 -> 已驳回（审核状态REJECTED + 业务状态ONGOING）
          if (item.auditStatus === 'REJECTED' && item.bizStatus === 'ONGOING') {
            filtered.push(item);
          }
          break;
        case 'RESOLVED':
          // 已找回 -> 失物找回（业务状态RESOLVED）
          if (item.bizStatus === 'RESOLVED') {
            filtered.push(item);
          }
          break;
        case 'RETURNED':
          // 已归还 -> 拾物归还（业务状态RETURNED）
          if (item.bizStatus === 'RETURNED') {
            filtered.push(item);
          }
          break;
        case 'CANCELLED':
          // 已取消 -> 已取消（业务状态CANCELLED）
          if (item.bizStatus === 'CANCELLED') {
            filtered.push(item);
          }
          break;
      }
    }
    return filtered;
  },

  onSwitchTab(e) {
    const status = e.currentTarget.dataset.status;
    const filteredItems = this.filterItems(this.data.allItems, status);
    
    this.setData({
      status: status,
      currentStatus: status,
      pageTitle: this.getStatusTitle(status),
      filteredItems: filteredItems
    });
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.isLoading) {
      this.loadNextPage();
    } else if (!this.data.hasMore && !this.data.noMoreData) {
      this.setData({ noMoreData: true });
      showToast('已无更多数据');
    }
  },

  onPullDownRefresh() {
    if (this.data.refreshing) {
      return;
    }
    
    this.setData({ refreshing: true });
    this.loadFirstPage();
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: '/pages/detail/detail?id=' + id
    });
  }
});
