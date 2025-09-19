import React, { useState, useMemo, useEffect } from 'react';
import axios from 'axios'; // axios 임포트
import {
    Box,
    CssBaseline,
    Grid,
    FormControl,
    Select,
    MenuItem
} from '@mui/material';

// --- 아이콘 및 공용 컴포넌트 임포트 ---
import Sidebar from '../components/layout/Sidebar';
import WorkspaceList from '../components/layout/WorkspaceList';
import SearchInput from '../components/common/SearchInput';
import Pagination from '../components/common/Pagination';
import TemplateCard from '../components/template/TemplateCard';
import CommonButton from '../components/button/CommonButton'; // <-- The missing import

// --- 목업 데이터 --- (이 부분은 나중에 제거)
// const mockPublicTemplates = Array.from({ length: 45 }, (_, i) => ({
//     id: i + 1,
//     title: `공용 템플릿 ${i + 1}`,
//     content: '템플릿 내용ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd',
//     status: ['심사중', '심사완료', '반려', '심사요청'][i % 4],
// }));

const ITEMS_PER_PAGE = 12;

// --- 최종 페이지 조립 ---
export default function PublicTemplatePage() {
    const [searchQuery, setSearchQuery] = useState('');
    const [currentPage, setCurrentPage] = useState(1);
    const [sortOrder, setSortOrder] = useState('최신 순');
    const [publicTemplates, setPublicTemplates] = useState([]); // 공용 템플릿 목록
    const [isLoading, setIsLoading] = useState(true); // 로딩 상태
    const [error, setError] = useState(null); // 에러 상태
    const [totalPages, setTotalPages] = useState(0); // 전체 페이지 수
    const [totalElements, setTotalElements] = useState(0); // 전체 템플릿 수

    const handleSearch = (query) => { setSearchQuery(query); setCurrentPage(1); };
    const handlePageChange = (event, value) => { setCurrentPage(value); };
    const handleSortChange = (event) => { setSortOrder(event.target.value); };

    useEffect(() => {
        const fetchPublicTemplates = async () => {
            setIsLoading(true);
            setError(null);
            try {
                const sortMapping = {
                    '최신 순': { sort: 'createdAt', direction: 'DESC' },
                    '공유 순': { sort: 'shareCount', direction: 'DESC' },
                    '가나다 순': { sort: 'publicTemplateTitle', direction: 'ASC' },
                };
                const currentSort = sortMapping[sortOrder] || { sort: 'createdAt', direction: 'DESC' };

                const accessToken = localStorage.getItem('accessToken'); // localStorage에서 토큰 가져오기

                if (!accessToken) {
                    setError(new Error('로그인이 필요합니다.'));
                    setIsLoading(false);
                    return;
                }

                const response = await axios.get(`http://localhost:8080/api/public-templates`,
                    {
                        params: {
                            page: currentPage - 1, // API는 0부터 시작하는 페이지 번호를 사용
                            size: ITEMS_PER_PAGE,
                            sort: currentSort.sort, // 분리된 sort 파라미터
                            direction: currentSort.direction, // 분리된 direction 파라미터
                        },
                        headers: {
                            'Authorization': `Bearer ${accessToken}`,
                        },
                    }
                );
                setPublicTemplates(response.data.content); // API 응답 형식에 따라 content 필드를 사용
                setTotalPages(response.data.totalPages);
                setTotalElements(response.data.totalElements);
            } catch (err) {
                setError(err);
                console.error("Failed to fetch public templates:", err);
            } finally {
                setIsLoading(false);
            }
        };

        fetchPublicTemplates();
    }, [searchQuery, currentPage, sortOrder]); // 의존성 배열에 searchQuery, currentPage, sortOrder 포함

    // --- 목업 데이터 제거 --- 이 부분은 완전히 제거될 거야.
    // const mockPublicTemplates = Array.from({ length: 45 }, (_, i) => ({
    //     id: i + 1,
    //     title: `공용 템플릿 ${i + 1}`,
    //     content: '템플릿 내용ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd',
    //     status: ['심사중', '심사완료', '반려', '심사요청'][i % 4],
    // }));

    // const ITEMS_PER_PAGE = 12; // 이 상수는 그대로 유지

    const finalFilteredTemplates = useMemo(() => {
        // API 연동 후에는 이 useMemo는 필요 없을 수 있지만, 일단은 유지
        // 클라이언트 측 필터링 대신 서버 측 필터링을 사용할 것이므로 이 로직은 변경될 수 있음
        return publicTemplates; // 이제 publicTemplates는 API로부터 오는 데이터
    }, [publicTemplates]);

    // const totalPages = Math.ceil(finalFilteredTemplates.length / ITEMS_PER_PAGE);
    // const paginatedTemplates = finalFilteredTemplates.slice(
    //     (currentPage - 1) * ITEMS_PER_PAGE,
    //     currentPage * ITEMS_PER_PAGE
    // );

    // API 연동 후에는 이 부분도 수정될 거야.
    // paginatedTemplates는 이제 publicTemplates와 동일하게 될 것임
    const paginatedTemplates = finalFilteredTemplates; // API 연동 후에는 이렇게 변경

    return (
        <Box sx={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
            <CssBaseline />

            <Sidebar>
                <WorkspaceList />
            </Sidebar>

            <Box component="main" sx={{ flexGrow: 1, p: 3, display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexShrink: 0 }}>
                    <CommonButton sx={{ bgcolor: '#343a40', color: 'white', boxShadow: 'none', '&:hover': { bgcolor: '#495057' } }}>템플릿 제작</CommonButton>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <SearchInput onSearch={handleSearch} />
                        <FormControl size="small" sx={{ minWidth: 120 }}>
                            <Select value={sortOrder} onChange={handleSortChange}>
                                <MenuItem value={'최신 순'}>최신 순</MenuItem>
                                <MenuItem value={'공유 순'}>공유 순</MenuItem>
                                <MenuItem value={'가나다 순'}>가나다 순</MenuItem>
                            </Select>
                        </FormControl>
                    </Box>
                </Box>

                {isLoading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flexGrow: 1 }}>
                        로딩 중...
                    </Box>
                ) : error ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flexGrow: 1, color: 'red' }}>
                        에러 발생: {error.message}
                    </Box>
                ) : (
                    <Box sx={{ width: '100%', flexGrow: 1, overflow: 'auto', p: 0.5 }}>
                        <Box
                            sx={{
                                display: 'flex',
                                flexWrap: 'wrap',
                            }}
                        >
                            {publicTemplates.map(template => (
                                <Box
                                    key={template.publicTemplateId} // API 응답에 따른 ID 필드 사용
                                    sx={{
                                        flex: '0 0 25%',    // 한 줄에 4개
                                        boxSizing: 'border-box',
                                        p: 1,               // 카드 사이 여백
                                    }}
                                >
                                    <TemplateCard template={template} isPublic={true} /> {/* isPublic prop 추가 */}
                                </Box>
                            ))}
                        </Box>
                    </Box>
                )}

                <Pagination
                    count={totalPages}
                    page={currentPage}
                    onChange={handlePageChange}
                />
            </Box>
        </Box>
    );
}
