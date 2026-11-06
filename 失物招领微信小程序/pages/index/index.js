// pages/index/index.js
const { get } = require('../../utils/request');
const { requireLogin } = require('../../utils/auth');
const { showToast, formatRelativeTime, getItemTypeText, getBizStatusText } = require('../../utils/util');
const { formatImageUrl } = require('../../utils/imageUrl');

Page({
  data: {
    list: [],
    page: 1,
    pageSize: 10,
    hasMore: true,
    loading: false,
    refreshing: false,
    
    // 顶部标签：捡到物品(FOUND)/丢失物品(LOST)
    activeTab: 'FOUND',
    
    // 搜索
    searchValue: '',
    keyword: '',
    
    // 分类筛选
    category: '',
    showCategoryFilter: false,
    tempCategory: '',
    filterCategoryText: '',
    
    // 时间筛选
    showTimeFilter: false,
    timeRange: '',
    tempTimeRange: '',
    filterTimeText: '',
    startTime: '',
    endTime: '',
    
    // 地点筛选
    location: '',
    showLocationFilter: false,
    tempLocation: '',
    filterLocationText: '',
    
    // 状态筛选
    status: '',
    showStatusFilter: false,
    tempStatus: '',
    filterStatusText: ''
  },

  onLoad() {
    // 尝试从本地存储恢复用户最后浏览的标签页
    const savedTab = wx.getStorageSync('index_activeTab');
    if (savedTab) {
      this.setData({ activeTab: savedTab });
    }
    
    requireLogin(() => {
      this.loadItems(true);
    });
  },

  onShow() {
    // 从其他页面返回时，刷新当前标签页的数据
    // 保持当前 activeTab 不变，确保用户看到的是之前浏览的标签页
    requireLogin(() => {
      this.loadItems(true);
      // 刷新 tabBar 小红点
      const app = getApp();
      if (app && app.loadUnreadCount) {
        app.loadUnreadCount();
      }
    });
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.setData({ refreshing: true });
    this.loadItems(true).then(() => {
      wx.stopPullDownRefresh();
      this.setData({ refreshing: false });
    });
  },

  // 上拉加载
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadItems(false);
    }
  },

  // 顶部标签切换
  onTabChange(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({
      activeTab: tab,
      page: 1,
      list: []
    });
    // 保存用户选择的标签页到本地存储
    wx.setStorageSync('index_activeTab', tab);
    this.loadItems(true);
  },

  // 搜索
  onSearch() {
    const value = this.data.searchValue;
    this.setData({
      keyword: value,
      page: 1
    });
    this.loadItems(true);
  },

  onSearchInput(e) {
    this.setData({ searchValue: e.detail.value });
  },

  onClearSearch() {
    this.setData({
      keyword: '',
      searchValue: '',
      page: 1
    });
    this.loadItems(true);
  },

  // 显示类型筛选
  onShowCategoryFilter() {
    this.setData({
      showCategoryFilter: true,
      tempCategory: this.data.category
    });
  },

  onHideCategoryFilter() {
    this.setData({ showCategoryFilter: false });
  },

  // 选择类型
  onSelectCategory(e) {
    const category = e.currentTarget.dataset.category;
    this.setData({ tempCategory: category });
  },

  // 确认类型筛选
  onConfirmCategoryFilter() {
    this.setData({
      category: this.data.tempCategory,
      filterCategoryText: this.data.tempCategory || '',
      showCategoryFilter: false,
      page: 1
    });

    this.loadItems(true);
  },

  // 显示时间筛选
  onShowTimeFilter() {
    this.setData({
      showTimeFilter: true,
      tempTimeRange: this.data.timeRange
    });
  },

  onHideTimeFilter() {
    this.setData({ showTimeFilter: false });
  },

  // 选择时间范围
  onSelectTimeRange(e) {
    const range = e.currentTarget.dataset.range;
    this.setData({ tempTimeRange: range });
  },

  // 确认时间筛选
  onConfirmTimeFilter() {
    const range = this.data.tempTimeRange;
    const now = new Date();
    let startTime = '';
    let endTime = '';
    let filterTimeText = '';

    if (range === 'today') {
      startTime = this.formatDate(new Date(now.setHours(0, 0, 0, 0)));
      endTime = this.formatDate(now);
      filterTimeText = '今天';
    } else if (range === 'week') {
      const weekStart = new Date(now);
      weekStart.setDate(now.getDate() - now.getDay() + 1);
      weekStart.setHours(0, 0, 0, 0);
      startTime = this.formatDate(weekStart);
      endTime = this.formatDate(now);
      filterTimeText = '本周';
    } else if (range === 'month') {
      const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
      startTime = this.formatDate(monthStart);
      endTime = this.formatDate(now);
      filterTimeText = '本月';
    }

    this.setData({
      timeRange: range,
      startTime,
      endTime,
      filterTimeText,
      showTimeFilter: false,
      page: 1
    });

    this.loadItems(true);
  },

  // 显示地点筛选
  onShowLocationFilter() {
    this.setData({
      showLocationFilter: true,
      tempLocation: this.data.location
    });
  },

  onHideLocationFilter() {
    this.setData({ showLocationFilter: false });
  },

  // 选择地点
  onSelectLocation(e) {
    const location = e.currentTarget.dataset.location;
    this.setData({ tempLocation: location });
  },

  // 确认地点筛选
  onConfirmLocationFilter() {
    this.setData({
      location: this.data.tempLocation,
      filterLocationText: this.data.tempLocation || '',
      showLocationFilter: false,
      page: 1
    });

    this.loadItems(true);
  },

  // 显示状态筛选
  onShowStatusFilter() {
    this.setData({
      showStatusFilter: true,
      tempStatus: this.data.status
    });
  },

  onHideStatusFilter() {
    this.setData({ showStatusFilter: false });
  },

  // 选择状态
  onSelectStatus(e) {
    const status = e.currentTarget.dataset.status;
    this.setData({ tempStatus: status });
  },

  // 确认状态筛选
  onConfirmStatusFilter() {
    const status = this.data.tempStatus;
    let filterStatusText = '';
    
    if (status === 'ONGOING') {
      filterStatusText = '进行中';
    } else if (status === 'COMPLETED') {
      filterStatusText = '已归还/已找回';
    }

    this.setData({
      status: status,
      filterStatusText: filterStatusText,
      showStatusFilter: false,
      page: 1
    });

    this.loadItems(true);
  },

  // 加载列表
  loadItems(isRefresh = false) {
    const { page, pageSize, activeTab, keyword, category, startTime, endTime, loading, location, status } = this.data;
    
    if (loading) return Promise.resolve();
    
    this.setData({ loading: true });
    
    const currentPage = isRefresh ? 1 : page;
    
    const params = {
      page: currentPage,
      pageSize
    };
    
    if (activeTab) params.itemType = activeTab;
    if (keyword) params.keyword = keyword;
    if (category) params.category = category;
    if (startTime) params.startTime = startTime;
    if (endTime) params.endTime = endTime;
    if (location) params.location = location;
    if (status) params.status = status;

    return get('/items', params)
      .then(res => {
        const { list, total, totalPage } = res.data;
        
        const formattedList = list.map(item => ({
          ...item,
          coverImage: formatImageUrl(item.coverImage),
          itemTypeText: getItemTypeText(item.itemType),
          bizStatusText: getBizStatusText(item.bizStatus, item.itemType, item.auditStatus),
          publishedAtText: formatRelativeTime(item.publishedAt)
        }));

        this.setData({
          list: isRefresh ? formattedList : [...this.data.list, ...formattedList],
          page: currentPage + 1,
          hasMore: currentPage < totalPage
        });
      })
      .catch(err => {
        console.error('加载列表失败:', err);
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  // 跳转详情
  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/detail/detail?id=${id}`
    });
  },

  // 阻止事件冒泡
  stopPropagation() {
    // 空函数，用于阻止事件冒泡
  },

  // 日期格式化
  formatDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    const h = String(date.getHours()).padStart(2, '0');
    const min = String(date.getMinutes()).padStart(2, '0');
    return `${y}-${m}-${d} ${h}:${min}`;
  }
});
