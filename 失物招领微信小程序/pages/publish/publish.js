// pages/publish/publish.js
const { post, get, patch, uploadImage } = require('../../utils/request');
const { requireLogin } = require('../../utils/auth');
const { showToast } = require('../../utils/util');
const { formatImageUrl } = require('../../utils/imageUrl');

const categories = ['证件卡片', '数码产品', '生活用品', '服饰鞋帽', '书籍文具', '其他'];
const locations = ['教学楼', '图书馆', '食堂', '宿舍区', '操场', '校门口'];

Page({
  data: {
    itemType: 'LOST',
    title: '',
    category: '',
    categories,
    dateValue: '',
    timeValue: '',
    happenTime: '',
    maxDate: '',
    maxTime: '',
    location: '',
    locationDetail: '',
    locations,
    featureDesc: '',
    images: [],
    loading: false,
    isEdit: false,
    editItemId: null,
    _hasLoadedEditData: false,
    _isChoosingImage: false
  },

  onLoad(options) {
    requireLogin(() => {
      console.log('publish页面onLoad参数:', options);

      this.refreshMaxDateTime();

      const app = getApp();

      // 如果存在表单草稿（从选择图片返回时页面可能被重载），恢复草稿
      if (app.globalData._publishDraft && app.globalData._publishDraftRestorable) {
        this._restoreFormDraft();
        // 标记表单已加载完成，阻止 onShow 中的 resetForm
        this.setData({ _hasLoadedEditData: true });
        app.globalData._publishDraftRestorable = false;
        // 草稿恢复后，继续处理编辑模式不适用，直接返回
        // 因为选择图片时已经是编辑或新建状态，不需要重新走 checkEditMode
        return;
      }

      this.setData({ category: categories[0] });

      this.checkEditMode(options);
    });
  },

  onShow() {
    const app = getApp();
    console.log('publish页面onShow, isChoosingImage(页面):', this.data._isChoosingImage, ', isChoosingImage(全局):', app.globalData._publishIsChoosingImage);

    this.refreshMaxDateTime();

    // 检查全局标志（页面重载时 this.data 会被重置，但 app.globalData 持久）
    if (app.globalData._publishIsChoosingImage) {
      app.globalData._publishIsChoosingImage = false;
      this.setData({ _isChoosingImage: false });
      // 恢复草稿（以防 onLoad 没被触发的场景）
      if (app.globalData._publishDraft) {
        this._restoreFormDraft();
        // 标记表单已加载完成
        this.setData({ _hasLoadedEditData: true });
      }
      return;
    }

    // 页面级标志：正常流程下从选图返回
    if (this.data._isChoosingImage) {
      this.setData({ _isChoosingImage: false });
      return;
    }

    const app2 = getApp();
    if (app2.globalData.editItemId && !this.data._hasLoadedEditData) {
      console.log('从全局变量进入编辑模式，物品ID:', app2.globalData.editItemId);
      const editItemId = app2.globalData.editItemId;
      app2.globalData.editItemId = null;
      this.setData({ _hasLoadedEditData: true });
      this.loadEditData(editItemId);
    } else if (!this.data.isEdit && !this.data._hasLoadedEditData) {
      this.resetForm();
    }

    // 最终兜底：检查草稿中的图片是否已被 success 回调更新
    // 如果草稿中图片多于当前页面（选择图片后页面重载，success 回调在 onLoad/onShow 之后才触发写入）
    const app3 = getApp();
    if (app3.globalData._publishDraftImagesUpdated && app3.globalData._publishDraft) {
      app3.globalData._publishDraftImagesUpdated = false;
      const draftImages = app3.globalData._publishDraft.images || [];
      const currentImages = this.data.images || [];

      // 以路径为准比对，仅同步未在当前页面中存在的图片
      const currentPaths = new Set(currentImages.map(img => img.path));
      const missingImages = draftImages.filter(img => !currentPaths.has(img.path));

      if (missingImages.length > 0) {
        const mergedImages = [...currentImages, ...missingImages];
        this.setData({ images: mergedImages });
        missingImages.forEach((img, i) => {
          const index = currentImages.length + i;
          if (img.status === 'uploading') {
            this.uploadSingleImage(img.path, index);
          }
        });
      }
    }
  },

  onUnload() {
    // 页面卸载时重置标志
    this.setData({ _hasLoadedEditData: false });
  },

  onHide() {
    // 页面隐藏时重置标志（从发布页切走后再切回来才会重新检查）
    // 但是不重置 _hasLoadedEditData，避免正在选择图片回来时重置表单
  },

  checkEditMode(options) {
    let editItemId = null;
    
    // 先从URL参数检查
    if (options.edit) {
      editItemId = options.edit;
      console.log('从URL参数进入编辑模式，物品ID:', editItemId);
    } 
    // 再从全局变量检查
    else {
      const app = getApp();
      if (app.globalData.editItemId) {
        editItemId = app.globalData.editItemId;
        console.log('从全局变量进入编辑模式，物品ID:', editItemId);
        // 清除全局变量防止重复触发
        app.globalData.editItemId = null;
      }
    }
    
    // 如果有编辑ID则加载编辑数据
    if (editItemId) {
      this.setData({ _hasLoadedEditData: true });
      this.loadEditData(editItemId);
    }
  },

  // 加载编辑数据
  async loadEditData(itemId) {
    console.log('开始加载编辑数据，物品ID:', itemId);
    this.setData({ loading: true });
    
    try {
      const res = await get(`/items/${itemId}`);
      console.log('获取物品详情成功:', res);
      const item = res.data;
      
      // 处理日期和时间
      let dateValue = '';
      let timeValue = '';
      if (item.happenTime) {
        const date = new Date(item.happenTime);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hour = String(date.getHours()).padStart(2, '0');
        const minute = String(date.getMinutes()).padStart(2, '0');
        dateValue = `${year}-${month}-${day}`;
        timeValue = `${hour}:${minute}`;
      }
      
      // 处理图片 - 直接使用 item.imageUrls（后端已经返回了完整URL）
      const images = (item.imageUrls || []).map(url => ({
        url: url, // 后端返回的已经是完整URL了
        status: 'success'
      }));

      // 解析 location 字段，分离出 location 和 locationDetail
      let location = '';
      let locationDetail = '';
      if (item.location) {
        // 查找是否包含预定义的地点
        for (const loc of locations) {
          if (item.location.startsWith(loc)) {
            location = loc;
            locationDetail = item.location.substring(loc.length).trim();
            break;
          }
        }
        // 如果没有匹配到预定义地点
        if (!location) {
          location = item.location;
          locationDetail = '';
        }
      }

      this.setData({
        isEdit: true,
        editItemId: itemId,
        itemType: item.itemType,
        title: item.title,
        category: item.category,
        dateValue,
        timeValue,
        happenTime: item.happenTime,
        location,
        locationDetail,
        featureDesc: item.featureDesc,
        images
      });
    } catch (err) {
      console.error('加载编辑数据失败:', err);
      showToast('加载失败，请重试');
    } finally {
      this.setData({ loading: false });
    }
  },

  refreshMaxDateTime() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hour = String(now.getHours()).padStart(2, '0');
    const minute = String(now.getMinutes()).padStart(2, '0');
    this.setData({
      maxDate: `${year}-${month}-${day}`,
      maxTime: `${hour}:${minute}`
    });
  },

  _saveFormDraft() {
    const app = getApp();
    app.globalData._publishDraft = {
      itemType: this.data.itemType,
      title: this.data.title,
      category: this.data.category,
      dateValue: this.data.dateValue,
      timeValue: this.data.timeValue,
      happenTime: this.data.happenTime,
      location: this.data.location,
      locationDetail: this.data.locationDetail,
      featureDesc: this.data.featureDesc,
      images: this.data.images.slice(),
      isEdit: this.data.isEdit,
      editItemId: this.data.editItemId,
      _hasLoadedEditData: this.data._hasLoadedEditData
    };
    app.globalData._publishIsChoosingImage = true;
    app.globalData._publishDraftRestorable = true;
  },

  _restoreFormDraft() {
    const app = getApp();
    const draft = app.globalData._publishDraft;
    if (!draft) return;

    this.setData({
      itemType: draft.itemType || 'LOST',
      title: draft.title || '',
      category: draft.category || categories[0],
      dateValue: draft.dateValue || '',
      timeValue: draft.timeValue || '',
      happenTime: draft.happenTime || '',
      location: draft.location || '',
      locationDetail: draft.locationDetail || '',
      featureDesc: draft.featureDesc || '',
      images: draft.images || [],
      isEdit: draft.isEdit || false,
      editItemId: draft.editItemId || null,
      _hasLoadedEditData: draft._hasLoadedEditData || false
    });

    // 重新触发 uploading 状态图片的上传（旧页面实例的上传可能已失效）
    (draft.images || []).forEach((img, index) => {
      if (img.status === 'uploading') {
        this.uploadSingleImage(img.path, index);
      }
    });
  },

  _clearFormDraft() {
    const app = getApp();
    app.globalData._publishDraft = null;
    app.globalData._publishIsChoosingImage = false;
    app.globalData._publishDraftRestorable = false;
    app.globalData._publishDraftImagesUpdated = false;
  },

  // 切换类型
  onTypeChange(e) {
    this.setData({ itemType: e.currentTarget.dataset.type });
  },

  // 输入处理
  onTitleInput(e) {
    this.setData({ title: e.detail.value });
  },

  onFeatureDescInput(e) {
    this.setData({ featureDesc: e.detail.value });
  },

  // 选择分类
  onCategoryChange(e) {
    this.setData({ category: categories[e.detail.value] });
  },

  // 选择地点
  onLocationChange(e) {
    this.setData({ location: locations[e.detail.value] });
  },

  onLocationDetailInput(e) {
    this.setData({ locationDetail: e.detail.value });
  },

  // 选择日期
  onDateChange(e) {
    const selectedDate = e.detail.value;
    this.refreshMaxDateTime();
    if (selectedDate > this.data.maxDate) {
      showToast('不能选择未来的日期');
      return;
    }

    let newTimeValue = this.data.timeValue;
    if (selectedDate === this.data.maxDate && this.data.timeValue && this.data.timeValue > this.data.maxTime) {
      newTimeValue = this.data.maxTime;
    }

    this.setData({
      dateValue: selectedDate,
      timeValue: newTimeValue
    });
    this.updateHappenTime();
  },

  // 选择时间
  onTimePickerChange(e) {
    const selectedTime = e.detail.value;
    this.refreshMaxDateTime();

    if (this.data.dateValue === this.data.maxDate && selectedTime > this.data.maxTime) {
      showToast('不能选择未来的时间');
      return;
    }

    this.setData({
      timeValue: selectedTime
    });
    this.updateHappenTime();
  },

  // 更新happenTime
  updateHappenTime() {
    const { dateValue, timeValue } = this.data;
    if (dateValue && timeValue) {
      this.setData({ happenTime: `${dateValue} ${timeValue}` });
    } else if (dateValue) {
      this.setData({ happenTime: dateValue });
    }
  },

  // 选择图片
  onChooseImage() {
    // 先保存当前表单到全局（防止页面重载导致数据丢失）
    this._saveFormDraft();
    this.setData({ _isChoosingImage: true });

    const that = this;
    const app = getApp();

    wx.chooseMedia({
      count: 9 - that.data.images.length,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success(res) {
        const newImages = res.tempFiles.map(file => ({
          path: file.tempFilePath,
          status: 'uploading'
        }));

        // 1. 更新 globalData 草稿（页面重载场景下，新页面实例会从此处读取）
        if (app.globalData._publishDraft) {
          app.globalData._publishDraft.images = [
            ...(app.globalData._publishDraft.images || []),
            ...newImages
          ];
        }
        // 2. 标记：草稿中的图片已更新，需要同步到页面
        app.globalData._publishDraftImagesUpdated = true;

        // 3. 更新当前页面数据（页面未重载时的正常流程）
        try {
          that.setData({
            images: [...that.data.images, ...newImages]
          });
          newImages.forEach((img, index) => {
            that.uploadSingleImage(img.path, that.data.images.length - newImages.length + index);
          });
        } catch (e) {
          console.error('页面实例已重载，图片将由 onLoad/onShow 从草稿恢复', e);
        }
      },
      fail() {
        that.setData({ _isChoosingImage: false });
        app.globalData._publishIsChoosingImage = false;
      },
      complete() {
        app.globalData._publishIsChoosingImage = false;
      }
    });
  },

  // 上传单张图片
  uploadSingleImage(filePath, index) {
    uploadImage(filePath)
      .then(res => {
        const images = this.data.images;
        images[index].status = 'success';
        images[index].url = res.data.url;
        this.setData({ images });
      })
      .catch(err => {
        const images = this.data.images;
        images[index].status = 'error';
        this.setData({ images });
        showToast('图片上传失败');
      });
  },

  // 预览图片
  onPreviewImage(e) {
    const index = e.currentTarget.dataset.index;
    const successImages = this.data.images.filter(img => img.status === 'success');
    const urls = successImages.map(img => img.url || img.path);
    wx.previewImage({
      current: urls[index],
      urls
    });
  },

  // 删除图片
  onDeleteImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = this.data.images;
    images.splice(index, 1);
    this.setData({ images });
  },

  // 发布
  onSubmit() {
    const { itemType, title, category, happenTime, location, locationDetail, featureDesc, images, loading } = this.data;
    
    if (loading) return;

    // 表单校验
    if (!title) {
      showToast('请输入标题');
      return;
    }

    if (!happenTime) {
      showToast('请选择时间');
      return;
    }

    // 校验 happenTime 不能是未来时间
    const inputDateTime = new Date(happenTime.replace(/-/g, '/')).getTime();
    if (inputDateTime > Date.now()) {
      showToast('不能选择未来的日期时间');
      return;
    }

    if (!location) {
      showToast('请选择地点');
      return;
    }

    // 合并地点和详细位置
    const fullLocation = locationDetail ? `${location} ${locationDetail}` : location;

    if (!featureDesc) {
      showToast('请输入特征描述');
      return;
    }

    // 检查图片上传状态
    const uploadingImages = images.filter(img => img.status === 'uploading');
    if (uploadingImages.length > 0) {
      showToast('图片上传中，请稍候');
      return;
    }

    this.setData({ loading: true });

    // 处理图片URL，确保保存原始路径（去掉服务器地址前缀）
    const imageUrls = images.filter(img => img.status === 'success').map(img => {
      let url = img.url;
      // 如果是完整URL，提取路径部分
      if (url.startsWith('http://') || url.startsWith('https://')) {
        try {
          const urlObj = new URL(url);
          url = urlObj.pathname;
          // 去掉开头的斜杠（如果有的话）
          if (url.startsWith('/')) {
            url = url.substring(1);
          }
        } catch (e) {
          // 不是有效的URL，保持原样
        }
      }
      return url;
    });
    
    const { isEdit, editItemId } = this.data;
    const request = isEdit ? 
      patch(`/items/${editItemId}`, {
        itemType,
        title,
        category,
        happenTime,
        location: fullLocation,
        featureDesc,
        imageUrls
      }) :
      post('/items', {
        itemType,
        title,
        category,
        happenTime,
        location: fullLocation,
        featureDesc,
        imageUrls
      });

    request.then(res => {
      showToast(isEdit ? '修改成功，等待审核' : '发布成功，等待审核', 'success');
      
      setTimeout(() => {
        // 重置表单状态
        this.resetForm();
        // 跳转到首页
        wx.switchTab({
          url: '/pages/index/index'
        });
      }, 1500);
    }).catch(err => {
      console.error(isEdit ? '修改失败:' : '发布失败:', err);
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  // 重置表单
  resetForm() {
    this._clearFormDraft();

    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hour = String(now.getHours()).padStart(2, '0');
    const minute = String(now.getMinutes()).padStart(2, '0');
    const defaultDate = `${year}-${month}-${day}`;
    const defaultTime = `${hour}:${minute}`;

    this.setData({
      itemType: 'LOST',
      title: '',
      category: categories[0],
      dateValue: defaultDate,
      timeValue: defaultTime,
      happenTime: `${defaultDate} ${defaultTime}`,
      location: '',
      locationDetail: '',
      featureDesc: '',
      images: [],
      loading: false,
      isEdit: false,
      editItemId: null,
      _hasLoadedEditData: false,
      _isChoosingImage: false
    });
    this.refreshMaxDateTime();
  }
});
