#!/bin/bash

# Message domain entities
sed -i '' 's/package com.xhackathon.server.entity;/package com.xhackathon.server.domain.message.entity;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/message/entity/MessageThread.java

# User domain
sed -i '' 's/package com.xhackathon.server.entity;/package com.xhackathon.server.domain.user.entity;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/user/entity/User.java
sed -i '' 's/package com.xhackathon.server.repository;/package com.xhackathon.server.domain.user.repository;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/user/repository/UserRepository.java
sed -i '' 's/package com.xhackathon.server.dto;/package com.xhackathon.server.domain.user.dto;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/user/dto/UserDto.java
sed -i '' 's/import com.xhackathon.server.entity.User;/import com.xhackathon.server.domain.user.entity.User;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/user/dto/UserDto.java
sed -i '' 's/import com.xhackathon.server.entity.User;/import com.xhackathon.server.domain.user.entity.User;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/user/repository/UserRepository.java

# Message repositories
sed -i '' 's/package com.xhackathon.server.repository;/package com.xhackathon.server.domain.message.repository;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/message/repository/MessageRepository.java
sed -i '' 's/package com.xhackathon.server.repository;/package com.xhackathon.server.domain.message.repository;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/message/repository/MessageThreadRepository.java
sed -i '' 's/import com.xhackathon.server.entity.Message;/import com.xhackathon.server.domain.message.entity.Message;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/message/repository/MessageRepository.java
sed -i '' 's/import com.xhackathon.server.entity.MessageThread;/import com.xhackathon.server.domain.message.entity.MessageThread;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/message/repository/MessageThreadRepository.java

# DTOs
sed -i '' 's/package com.xhackathon.server.dto;/package com.xhackathon.server.domain.message.dto;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/message/dto/MessageDto.java
sed -i '' 's/package com.xhackathon.server.dto;/package com.xhackathon.server.domain.message.dto;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/message/dto/ThreadDto.java
sed -i '' 's/import com.xhackathon.server.entity.Message;/import com.xhackathon.server.domain.message.entity.Message;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/message/dto/MessageDto.java

# Request/Response DTOs
sed -i '' 's/package com.xhackathon.server.dto.request;/package com.xhackathon.server.domain.message.dto.request;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/message/dto/request/CreateThreadRequest.java
sed -i '' 's/package com.xhackathon.server.dto.request;/package com.xhackathon.server.domain.message.dto.request;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/message/dto/request/SendMessageRequest.java
sed -i '' 's/package com.xhackathon.server.dto.response;/package com.xhackathon.server.domain.message.dto.response;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/message/dto/response/*.java

# WebSocket
sed -i '' 's/package com.xhackathon.server.websocket;/package com.xhackathon.server.domain.message.websocket;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/domain/message/websocket/MessageWebSocketHandler.java

# Common
sed -i '' 's/package com.xhackathon.server.config;/package com.xhackathon.server.common.config;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/common/config/*.java
sed -i '' 's/package com.xhackathon.server.dto;/package com.xhackathon.server.common.dto;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/common/dto/WebSocketMessage.java
sed -i '' 's/package com.xhackathon.server.controller;/package com.xhackathon.server.common;/g' /Users/dongminbaek/Documents/1122/X-hackethon/server/src/main/java/com/xhackathon/server/common/HealthController.java

echo "Package names fixed!"