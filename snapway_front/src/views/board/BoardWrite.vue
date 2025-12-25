<template>
  <div class="write-container">
    <div class="write-card">
      <h2 class="write-title">{{ isEditMode ? '게시글 수정' : '게시글 작성' }}</h2>

      <form @submit.prevent="onSubmit" class="write-form">
        <div class="form-row options-row">
          <div class="form-group category-group">
            <label>카테고리</label>
            <select 
              v-model="form.category" 
              class="form-select"
              :disabled="isTripMode" 
            >
              <option value="" disabled>카테고리 선택</option>
              <option 
                v-for="opt in filteredCategories" 
                :key="opt.value" 
                :value="opt.value"
              >
                {{ opt.label }}
              </option>
            </select>
          </div>

          <div class="form-group visibility-group">
            <label 
              class="checkbox-label"
              :class="{ disabled: !canChangeVisibility }"
            >
              <input 
                type="checkbox" 
                v-model="isPrivate"
                :disabled="!canChangeVisibility"
              />
              <span class="custom-check"></span>
              나만 보기 (비공개)
            </label>
          </div>
        </div>

        <div class="form-group">
          <input
            v-model="form.title"
            type="text"
            class="form-input title-input"
            placeholder="제목을 입력하세요"
          />
        </div>

        <div class="editor-wrapper">
          <div ref="editorRef"></div>
        </div>

        <div class="form-actions">
          <button type="button" class="btn cancel" @click="goBack">취소</button>
          <button 
            type="submit" 
            class="btn submit"
            :disabled="!isValid" 
            :class="{ active: isValid }"
          >
            {{ isEditMode ? '수정 완료' : '등록하기' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Editor from '@toast-ui/editor';
import '@toast-ui/editor/dist/toastui-editor.css';

// API
import { createArticle, fetchArticle, updateArticle, uploadArticleImage } from '@/api/articleApi';
import { useAuthStore } from '@/store/useAuthStore';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// --- 상태 변수 ---
const editorRef = ref(null);
let editorInstance = null;

const form = reactive({
  title: '',
  category: '',
  visibility: 'PUBLIC',
  tags: '',
});

// 여행 기록 관련 ID (URL 쿼리 or 기존 게시글 태그에서 추출)
const tripId = ref(null);

// 카테고리 목록 정의
const allCategories = [
  { value: 'review', label: '여행 기록' }, // 여행 기록은 tripId가 있을 때만 사용
  { value: 'free', label: '자유' },
  { value: 'qna', label: '질문' },
  { value: 'mate', label: '동행 구하기' },
  { value: 'tip', label: '여행 팁' },
];

// --- Computed Properties ---

// 수정 모드 여부 확인
const isEditMode = computed(() => !!route.params.articleId);

// "여행 기록 모드"인지 판별
// 1. 글 작성 시 URL 쿼리로 tripId가 넘어왔거나
// 2. 수정 시 기존 카테고리가 '여행 기록'인 경우 (또는 태그에 trip ID가 있는 경우)
const isTripMode = computed(() => {
  return !!tripId.value || form.category === 'review';
});

// [조건 2] 카테고리 필터링
// 여행 기록 모드이면 -> '여행 기록'만 표시
// 일반 모드이면 -> '여행 기록'을 제외한 나머지 표시
const filteredCategories = computed(() => {
  if (isTripMode.value) {
    return allCategories.filter(c => c.value === 'review');
  } else {
    return allCategories.filter(c => c.value !== 'review');
  }
});

// [조건 3] 공개 여부 변경 가능 여부
// 카테고리가 '여행 기록'일 때만 true
const canChangeVisibility = computed(() => form.category === 'review');

// 나만 보기 체크박스 바인딩 (Boolean <-> String 변환)
const isPrivate = computed({
  get: () => form.visibility === 'PRIVATE',
  set: (val) => { form.visibility = val ? 'PRIVATE' : 'PUBLIC'; }
});

// [문제 해결 1] 유효성 검사 (수정 버튼 활성화용)
// 제목, 내용, 카테고리만 있으면 OK. 파일 변경 여부는 무관.
const isValid = computed(() => {
  // 에디터 인스턴스가 없으면 내용 확인 불가하므로 false
  const content = editorInstance ? editorInstance.getMarkdown().trim() : '';
  return form.title.trim().length > 0 && 
         form.category.length > 0 && 
         content.length > 0;
});

// --- Watchers ---

// 카테고리가 변경될 때, '여행 기록'이 아니면 강제로 PUBLIC으로 전환
watch(() => form.category, (newVal) => {
  if (newVal !== 'review') {
    form.visibility = 'PUBLIC';
  }
});

// --- Methods ---

// Toast UI Editor 초기화
const initEditor = (initialContent = '') => {
  if (!editorRef.value) return;

  editorInstance = new Editor({
    el: editorRef.value,
    height: '500px',
    initialEditType: 'wysiwyg', // or 'markdown'
    previewStyle: 'vertical',
    initialValue: initialContent,
    hooks: {
      // 이미지 업로드 훅
      addImageBlobHook: async (blob, callback) => {
        try {
          // 백엔드 업로드 API 호출
          const { data } = await uploadArticleImage(blob, authStore.user?.id);
          // 에디터에 이미지 URL 삽입
          callback(data.fileUrl, 'image');
        } catch (error) {
          console.error('이미지 업로드 실패:', error);
          alert('이미지 업로드에 실패했습니다.');
        }
      },
    },
  });
  
  // 에디터 내용 변경 시 유효성 검사를 위해 강제 업데이트 트리거가 필요할 수 있음
  // 여기서는 isValid computed 속성이 호출될 때 getMarkdown()을 가져오도록 처리함.
  // Vue 반응성을 위해 이벤트를 달아주는 것이 좋음.
  editorInstance.on('change', () => {
    // 꼼수: form의 무의미한 값을 건드려 computed 재계산 유도 (선택사항)
    form.tags = form.tags + ' '; 
    form.tags = form.tags.trim();
  });
};

// 기존 데이터 불러오기 (수정 모드)
const loadArticleData = async () => {
  const articleId = route.params.articleId;
  try {
    const { data } = await fetchArticle(articleId);
    const article = data.article;

    // 본인 확인
    if (Number(article.authorId) !== Number(authStore.user?.id)) {
      alert('수정 권한이 없습니다.');
      router.replace({ name: 'board' });
      return;
    }

    form.title = article.title;
    form.category = article.category; // 여기서 'review'가 들어오면 isTripMode가 true가 됨
    form.visibility = article.visibility;
    form.tags = article.tags;
    
    // 태그에서 Trip ID 추출
    const match = String(article.tags || '').match(/trip:(\d+)/);
    if (match) {
      tripId.value = match[1];
    }

    // 에디터 초기화 (내용 주입)
    initEditor(article.content);

  } catch (error) {
    console.error('게시글 불러오기 실패:', error);
    alert('게시글 정보를 불러오지 못했습니다.');
    router.back();
  }
};

// 게시글 등록/수정 제출
const onSubmit = async () => {
  if (!isValid.value) return;
  if (!authStore.isLoggedIn) {
    alert('로그인이 필요합니다.');
    return;
  }

  const content = editorInstance.getMarkdown();
  
  // 태그 처리: tripId가 있으면 태그에 자동 추가
  let finalTags = form.tags || '';
  if (tripId.value && !finalTags.includes(`trip:${tripId.value}`)) {
    finalTags = finalTags ? `${finalTags},trip:${tripId.value}` : `trip:${tripId.value}`;
  }

  try {
    if (isEditMode.value) {
      // --- 수정 로직 (JSON 전송) ---
      const updateData = {
        articleId: Number(route.params.articleId),
        title: form.title,
        content: content,
        category: form.category,
        visibility: form.visibility,
        tags: finalTags,
        // authorId는 백엔드에서 세션으로 처리하지만 DTO 매칭을 위해 보낼 수도 있음
      };
      
      await updateArticle(updateData);
      alert('게시글이 수정되었습니다.');
      router.push({ name: 'boardDetail', params: { articleId: route.params.articleId } });

    } else {
      // --- 작성 로직 (FormData 전송) ---
      // 백엔드 saveArticle이 @RequestParam과 MultipartFile을 받으므로 FormData 사용
      const formData = new FormData();
      formData.append('title', form.title);
      formData.append('content', content); // 마크다운 텍스트
      formData.append('category', form.category);
      formData.append('tags', finalTags);
      formData.append('visibility', form.visibility);
      
      // 썸네일용 이미지가 필요하다면 여기서 추가 (현재 로직엔 없음 -> 빈 파일 혹은 생략)
      // formData.append('image', null); 

      await createArticle(formData);
      alert('게시글이 등록되었습니다.');
      router.push({ name: 'board' });
    }
  } catch (error) {
    console.error('저장 실패:', error);
    alert('저장 중 오류가 발생했습니다.');
  }
};

const goBack = () => {
  router.back();
};

onMounted(() => {
  // 1. URL 쿼리 파라미터 확인 (여행 기록에서 넘어왔는지)
  if (route.query.tripId) {
    tripId.value = route.query.tripId;
    form.category = 'review'; // 여행 기록 모드면 카테고리 자동 선택
  }

  // 2. 모드에 따라 초기화
  if (isEditMode.value) {
    loadArticleData();
  } else {
    // 작성 모드일 때 에디터 초기화
    initEditor();
  }
});
</script>

<style scoped>
.write-container {
  min-height: calc(100vh - 80px);
  padding: 40px 20px;
  background: #f1f5f9;
  display: flex;
  justify-content: center;
}

.write-card {
  width: 100%;
  max-width: 1000px;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.write-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  padding-bottom: 16px;
  border-bottom: 2px solid #e2e8f0;
}

.write-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: flex;
  gap: 20px;
  align-items: center;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.form-group label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #475569;
}

.category-group {
  width: 200px;
}

.form-select, .form-input {
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.95rem;
  outline: none;
  transition: all 0.2s;
}
.form-select:focus, .form-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.form-select:disabled {
  background: #f1f5f9;
  color: #64748b;
  cursor: not-allowed;
}

.title-input {
  font-size: 1.1rem;
  padding: 12px;
}

/* Visibility Checkbox Custom Style */
.visibility-group {
  justify-content: flex-end; /* 라벨 위치 맞춤 */
  height: 100%;
  padding-top: 28px; /* 라벨 높이만큼 여백 */
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}
.checkbox-label.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.checkbox-label input {
  display: none;
}
.custom-check {
  width: 20px;
  height: 20px;
  border: 2px solid #cbd5e1;
  border-radius: 4px;
  position: relative;
  transition: all 0.2s;
}
.checkbox-label input:checked + .custom-check {
  background: #3b82f6;
  border-color: #3b82f6;
}
.checkbox-label input:checked + .custom-check::after {
  content: '✔';
  color: #fff;
  font-size: 14px;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.editor-wrapper {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  overflow: hidden;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}

.btn {
  padding: 10px 24px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  font-size: 0.95rem;
  transition: all 0.2s;
}

.btn.cancel {
  background: #e2e8f0;
  color: #475569;
}
.btn.cancel:hover {
  background: #cbd5e1;
}

.btn.submit {
  background: #94a3b8;
  color: #fff;
  cursor: not-allowed;
}
.btn.submit.active {
  background: #3b82f6;
  cursor: pointer;
}
.btn.submit.active:hover {
  background: #2563eb;
}

@media (max-width: 768px) {
  .write-container { padding: 20px 10px; }
  .write-card { padding: 20px; }
  .form-row { flex-direction: column; align-items: stretch; }
  .category-group { width: 100%; }
  .visibility-group { padding-top: 0; }
}
</style>