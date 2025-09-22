import React, { useState, useMemo, useEffect } from 'react';
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
import CommonButton from '../components/button/CommonButton';

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
    const [userRole, setUserRole] = useState(null); // 사용자 역할 상태 추가

    const handleSearch = (query) => { setSearchQuery(query); setCurrentPage(1); };
    const handlePageChange = (event, value) => { setCurrentPage(value); };
    const handleSortChange = (event) => { setSortOrder(event.target.value); };

    useEffect(() => {
        // 로컬 스토리지에서 사용자 역할 로드
        const storedUserRole = localStorage.getItem('userRole');
        if (storedUserRole) {
            setUserRole(storedUserRole);
        }
    }, []); // 컴포넌트 마운트 시 한 번만 실행

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

                const accessToken = localStorage.getItem('accessToken');

                if (!accessToken) {
                    setError(new Error('로그인이 필요합니다.'));
                    setIsLoading(false);
                    return;
                }

                const params = new URLSearchParams({
                    page: currentPage - 1,
                    size: ITEMS_PER_PAGE,
                    sort: currentSort.sort,
                    direction: currentSort.direction,
                });

                // searchQuery는 백엔드에 검색 기능이 구현될 때 추가
                // if (searchQuery) {
                //     params.append('searchQuery', searchQuery);
                // }

                const response = await fetch(`http://localhost:8080/api/public-templates?${params.toString()}`, {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${accessToken}`,
                        'Content-Type': 'application/json',
                    },
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || '공용 템플릿 로딩 실패');
                }

                const data = await response.json();
                setPublicTemplates(data.content); // API 응답 형식에 따라 content 필드를 사용
                setTotalPages(data.totalPages);
                setTotalElements(data.totalElements);
            } catch (err) {
                setError(err);
                console.error("Failed to fetch public templates:", err);
            } finally {
                setIsLoading(false);
            }
        };

        fetchPublicTemplates();
    }, [searchQuery, currentPage, sortOrder]); // 의존성 배열에 searchQuery, currentPage, sortOrder 포함

    // 클라이언트 측 필터링 및 페이지네이션 로직 제거
    const finalFilteredTemplates = useMemo(() => {
        return publicTemplates; // 이제 publicTemplates는 API로부터 오는 데이터
    }, [publicTemplates]);

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
                                    <TemplateCard template={template} isPublic={true} userRole={userRole} /> {/* userRole prop 추가 */}
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
