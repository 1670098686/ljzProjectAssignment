import 'package:flutter/material.dart';

import '../errors/error_center.dart';
import '../network/app_exception.dart';

enum ViewState { idle, busy, error, success }

class BaseProvider extends ChangeNotifier {
  BaseProvider({ErrorCenter? errorCenter}) : _errorCenter = errorCenter;

  final ErrorCenter? _errorCenter;
  ViewState _state = ViewState.idle;
  String? _errorMessage;

  ViewState get state => _state;
  String? get errorMessage => _errorMessage;

  void setState(ViewState viewState) {
    // 只有当状态实际变化时才通知监听器
    if (_state != viewState) {
      _state = viewState;
      notifyListeners();
    }
  }

  void setError(dynamic error, {RetryCallback? retry}) {
    if (error is AppException) {
      _errorMessage = error.message;
    } else {
      _errorMessage = error.toString();
    }
    _errorCenter?.showError(
      message: _errorMessage ?? '未知错误',
      retry: retry,
    );
    setState(ViewState.error);
  }

  void setBusy() {
    _errorMessage = null;
    setState(ViewState.busy);
  }

  void setIdle() {
    setState(ViewState.idle);
  }

  bool get isLoading => _state == ViewState.busy;
  bool get hasError => _state == ViewState.error;
  bool get isSuccess => _state == ViewState.success;
}
