import { Link } from "react-router-dom";
import { Button } from "@/shared";

export function NotFoundPage() {
  return <main className="grid min-h-screen place-items-center bg-slate-50 px-5 text-center"><div><p className="text-8xl font-black text-brand-100">404</p><h1 className="mt-3 text-3xl font-extrabold">Không tìm thấy trang</h1><p className="mt-3 text-muted">Đường dẫn bạn truy cập không tồn tại hoặc đã được thay đổi.</p><Link to="/"><Button className="mt-7">Về trang chủ</Button></Link></div></main>;
}
