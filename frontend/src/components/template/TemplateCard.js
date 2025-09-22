import * as React from 'react';
import { Card, Box, Typography, Button, CardContent } from '@mui/material';

export default function TemplateCard({ template, isPublic = false, userRole }) { // userRole prop으로 변경
    // 상태에 따른 스타일을 정의합니다.
    const statusStyles = {
        '심사중': { bgcolor: '#e3f2fd', color: '#1565c0' },
        '심사완료': { bgcolor: '#616161', color: '#ffffff' },
        '반려': { bgcolor: 'white', color: 'black', borderTop: '1px solid #ccc' },
        '심사요청': { bgcolor: '#e8eaf6', color: '#303f9f' },
    };

    return (
        <Card
            variant="outlined"
            sx={{
                width: '100%',
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                borderColor: '#ccc',
            }}
        >
            <Box sx={{ p: 1, borderBottom: '1px solid #ccc', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h6" fontWeight="bold">{template.publicTemplateTitle}</Typography>
                <Box>
                    <Button size="small" variant="text" sx={{ color: 'black', minWidth: 'auto', p: 0.5 }}>즐겨찾기</Button>
                    <Button size="small" variant="text" sx={{ color: 'black', minWidth: 'auto', p: 0.5 }}>공유</Button>
                    <Button size="small" variant="text" sx={{ color: 'black', minWidth: 'auto', p: 0.5 }}>수정하기</Button>
                    {(!isPublic || (isPublic && userRole === 'ADMIN')) && (
                        <Button size="small" variant="text" sx={{ color: 'black', minWidth: 'auto', p: 0.5 }}>삭제</Button>
                    )}
                </Box>
            </Box>
            <CardContent sx={{ wordBreak: 'break-word', bgcolor: '#f8f9fa' }}>
                <Typography color="text.secondary">{template.publicTemplateContent}</Typography>
            </CardContent>

            {/* buttonTitle은 항상 content 아래에 위치하며, 값이 있을 때만 렌더링 */}
            {template.buttonTitle && (
                <Box sx={{ p: 1.5, textAlign: 'center', bgcolor: '#e3f2fd', color: '#1565c0' }}>
                    <Typography variant="body2" fontWeight="bold">{template.buttonTitle}</Typography>
                </Box>
            )}

            {/* 개인 템플릿일 경우 (isPublic이 false일 경우) buttonTitle 아래에 status 렌더링 */}
            {!isPublic && template.status && (
                <Box sx={{ p: 1.5, textAlign: 'center', ...statusStyles[template.status] }}>
                    <Typography variant="body2" fontWeight="bold">{template.status}</Typography>
                </Box>
            )}
        </Card>
    );
}