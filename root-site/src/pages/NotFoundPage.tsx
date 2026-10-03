import { Button, Result } from "antd";
import { useNavigate } from "react-router-dom";

export default function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <Result
      status="404"
      title="页面未找到"
      subTitle="这个地址没有对应的页面"
      extra={<Button type="primary" onClick={() => navigate("/")}>返回首页</Button>}
    />
  );
}
