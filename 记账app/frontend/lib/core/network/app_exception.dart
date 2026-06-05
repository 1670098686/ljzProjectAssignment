class AppException implements Exception {
  final String message;
  final int? code;

  AppException(this.message, [this.code]);

  @override
  String toString() => 'AppException(code: $code, message: $message)';
}

class NetworkException extends AppException {
  NetworkException(super.message, [super.code]);
}

class BadRequestException extends AppException {
  BadRequestException(super.message, [super.code]);
}

class UnauthorizedException extends AppException {
  UnauthorizedException(super.message, [super.code]);
}

class NotFoundException extends AppException {
  NotFoundException(super.message, [super.code]);
}

class ServerException extends AppException {
  ServerException(super.message, [super.code]);
}

class BusinessException extends AppException {
  BusinessException(super.message, [super.code]);
}
