<!-- src/components/Footer.vue -->
<template>
    <footer class="footer">
        <div class="footer-inner">
            <div class="footer-left">
                <span class="brand">SNAPWAY</span>
                <span class="divider">•</span>
                <span class="copy">© {{ new Date().getFullYear() }} SNAPWAY. All rights reserved.</span>
            </div>

            <div class="footer-right">
                <a href="#" class="footer-link">서비스 소개</a>
                <a href="#" class="footer-link">이용 약관</a>
                <a href="#" class="footer-link">개인정보 처리방침</a>
            </div>
        </div>
    </footer>
</template>

<script setup>
// 별도 로직 없음
</script>

<style scoped>
.footer {
    width: 100%;
    border-top: 1px solid rgba(148, 163, 184, 0.35);
    background: rgba(15, 23, 42, 0.9);
    backdrop-filter: blur(14px) saturate(140%);
    -webkit-backdrop-filter: blur(14px) saturate(140%);
    box-shadow: 0 -8px 24px rgba(15, 23, 42, 0.55);
    color: #e5e7eb;
}

.footer-inner {
    max-width: 1120px;
    margin: 0 auto;
    padding: 14px 20px 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    font-size: 0.85rem;
}

.footer-left {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

.brand {
    font-weight: 800;
    letter-spacing: 0.07em;
    font-size: 0.9rem;
    color: #bfdbfe;
}

.divider {
    opacity: 0.6;
}

.copy {
    color: #9ca3af;
}

.footer-right {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-wrap: wrap;
}

.footer-link {
    color: #9ca3af;
    text-decoration: none;
    position: relative;
    padding-bottom: 2px;
    transition: color 0.16s ease-out;
}

.footer-link::after {
    content: "";
    position: absolute;
    left: 0;
    bottom: 0;
    width: 0;
    height: 1px;
    background: linear-gradient(135deg, #38bdf8, #6366f1);
    transition: width 0.18s ease-out;
}

.footer-link:hover {
    color: #e5f0ff;
}

.footer-link:hover::after {
    width: 100%;
}

@media (max-width: 768px) {
    .footer-inner {
        flex-direction: column;
        align-items: flex-start;
    }

    .footer-right {
        gap: 10px;
    }
}
</style>
