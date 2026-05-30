package com.enterprise.knowledge.service;

import com.enterprise.knowledge.application.service.RagService;
import com.enterprise.knowledge.domain.DocumentMetadata;
import com.enterprise.knowledge.infrastructure.document.DocumentParser;
import com.enterprise.knowledge.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AsyncDocumentService {

    private final DocumentParser documentParser;
    private final RagService ragService;

    @Value("${knowledge.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${knowledge.rag.chunk-overlap:100}")
    private int chunkOverlap;

    @Async
    @Transactional
    public CompletableFuture<DocumentMetadata> processDocument(MultipartFile file, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentMetadata metadata = ragService.uploadDocument(file, userId);
                
                String content = documentParser.parse(file);
                ragService.addDocumentChunks(metadata.getId(), content);
                
                return metadata;
                
            } catch (Exception e) {
                throw new BusinessException("DOC_PROCESS_ERROR", "文档处理失败：" + e.getMessage());
            }
        });
    }

    public String parseDocumentContent(MultipartFile file) throws Exception {
        String content = documentParser.parse(file);
        
        if (content != null && content.length() > 5000) {
            content = content.substring(0, 5000) + "\n\n[...内容过长，已截断]";
        }
        
        return content;
    }
}
