// pages/chat/chat.js
const { get, post } = require('../../utils/request');
const { requireLogin, getUserInfo } = require('../../utils/auth');
const { uploadImage } = require('../../utils/request');
const { showToast } = require('../../utils/util');
const { formatImageUrl } = require('../../utils/imageUrl');

Page({
  data: {
    conversationId: null,
    conversation: null,
    item: null,
    messages: [],
    inputMessage: '',
    loading: false,
    sending: false,
    currentUserId: null,
    currentUserNickname: '',
    currentUserAvatarChar: '我',
    otherUserId: null,
    otherUserNickname: '',
    otherUserAvatarChar: '用'
  },

  onLoad(options) {
    requireLogin(() => {
      const { id } = options;
      if (id) {
        const user = getUserInfo();
        const nickname = user?.nickName || user?.nickname || '用户';
        const avatarChar = nickname.charAt(0) || '我';
        this.setData({ 
          conversationId: id,
          currentUserId: user?.id,
          currentUserNickname: nickname,
          currentUserAvatarChar: avatarChar
        });
        this.loadConversation();
      }
    });
  },

  onHide() {
    // 隐藏聊天页面时（如切换到其他小程序、被电话打断），立即更新未读数
    const app = getApp();
    if (app && app.loadUnreadCount) {
      app.loadUnreadCount();
    }
  },

  onUnload() {
    // 离开聊天页面返回消息页时，确保全局未读数和 tabBar badge 刷新
    const app = getApp();
    if (app && app.loadUnreadCount) {
      app.loadUnreadCount();
    }
  },

  loadConversation() {
    this.setData({ loading: true });

    get(`/messages/conversations/${this.data.conversationId}`)
      .then(res => {
        const conversation = res.data;
        const currentUser = getUserInfo();
        const isUser1 = conversation.user1Id === currentUser.id;
        const otherUser = isUser1 ? conversation.user2 : conversation.user1;

        const otherNickname = otherUser?.nickName || otherUser?.nickname || otherUser?.nickname || '用户';
        const currentNickname = currentUser?.nickName || currentUser?.nickname || '用户';
        const currentAvatarChar = currentNickname.charAt(0) || '我';
        const otherAvatarChar = otherNickname.charAt(0) || '用';

        this.setData({
          conversation,
          currentUserId: currentUser.id,
          currentUserNickname: currentNickname,
          currentUserAvatarChar: currentAvatarChar,
          otherUserId: otherUser?.id,
          otherUserNickname: otherNickname,
          otherUserAvatarChar: otherAvatarChar
        });

        if (conversation.itemId) {
          this.loadItem(conversation.itemId);
        } else {
          this.loadMessages();
        }
      })
      .catch(err => {
        console.error('加载会话失败：', err);
        this.setData({ loading: false });
      });
  },

  loadItem(itemId) {
    get(`/items/${itemId}`)
      .then(res => {
        const item = res.data;
        // 格式化物品图片 URL
        if (item.imageUrls && Array.isArray(item.imageUrls)) {
          item.imageUrls = item.imageUrls.map(url => formatImageUrl(url));
        }
        this.setData({ 
          item 
        });
        this.loadMessages();
      })
      .catch(err => {
        console.error('加载物品信息失败：', err);
        this.loadMessages();
      });
  },

  loadMessages() {
    get(`/messages/conversations/${this.data.conversationId}/messages`)
      .then(res => {
        const messages = (res.data || []).map(msg => ({
          ...msg,
          senderId: msg.sender_id,
          type: msg.type,
          content: msg.content,
          imageUrl: formatImageUrl(msg.image_url),
          createdAt: msg.created_at
        }));
        this.setData({ messages });
        this.scrollToBottom();

        const app = getApp();
        app.loadUnreadCount();
      })
      .catch(err => {
        console.error('加载消息失败：', err);
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  onMessageInput(e) {
    this.setData({ inputMessage: e.detail.value });
  },

  // 发送文本消息
  onSendMessage() {
    const { inputMessage, conversationId, sending } = this.data;
    
    if (sending) return;
    
    if (!inputMessage.trim()) {
      showToast('请输入消息内容');
      return;
    }

    this.setData({ sending: true });

    post(`/messages/conversations/${conversationId}/messages`, {
      messageType: 'TEXT',
      content: inputMessage
    }).then(res => {
      this.setData({
        inputMessage: ''
      });
      this.loadMessages();
    }).catch(err => {
      console.error('发送消息失败:', err);
    }).finally(() => {
      this.setData({ sending: false });
    });
  },

  // 发送图片
  onChooseImage() {
    const that = this;
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success(res) {
        const filePath = res.tempFiles[0].tempFilePath;
        that.sendImage(filePath);
      }
    });
  },

  sendImage(filePath) {
    this.setData({ sending: true });

    uploadImage(filePath)
      .then(uploadRes => {
        const imageUrl = uploadRes.data.url;
        
        post(`/messages/conversations/${this.data.conversationId}/messages`, {
          messageType: 'IMAGE',
          imageUrl
        }).then(res => {
          this.loadMessages();
        }).catch(err => {
          console.error('发送图片失败:', err);
        });
      })
      .catch(err => {
        showToast('图片上传失败');
      })
      .finally(() => {
        this.setData({ sending: false });
      });
  },

  scrollToBottom() {
    setTimeout(() => {
      this.setData({
        scrollTop: 99999
      });
    }, 100);
  },

  goToItemDetail() {
    if (this.data.item) {
      wx.navigateTo({
        url: `/pages/detail/detail?id=${this.data.item.id}`
      });
    }
  },

  onPreviewImage(e) {
    const url = e.currentTarget.dataset.url;
    wx.previewImage({
      urls: [url],
      current: url
    });
  }
});
