import React, { useState } from "react";
import { Link, NavLink, Route, Routes } from "react-router-dom";
const requests = [
  ["Nguyễn Thảo Vy", "Lo âu và căng thẳng", "22/07/2026 · 09:00", "Mới"],
  ["Trần Quốc Bảo", "Khó ngủ kéo dài", "22/07/2026 · 14:30", "Đã xác nhận"],
  ["Lê Mai Anh", "Vấn đề mối quan hệ", "23/07/2026 · 10:00", "Mới"],
];
function Logo() {
  return (
    <Link className="logo" to="/">
      <i>♥</i>MindCare
    </Link>
  );
}
function Shell({ children }) {
  return (
    <div className="shell">
      <aside className="sidebar">
        <Logo />
        <nav>
          <NavLink to="/">
            <i>⌂</i>Dashboard
          </NavLink>
          <NavLink to="/calendar">
            <i>▦</i>Lịch làm việc
          </NavLink>
          <NavLink to="/requests">
            <i>◫</i>Yêu cầu đặt lịch
          </NavLink>
          <NavLink to="/profile">
            <i>♙</i>Hồ sơ chuyên gia
          </NavLink>
          <NavLink to="/statistics">
            <i>↗</i>Thống kê
          </NavLink>
        </nav>
        <div className="side-bottom">
          <NavLink to="/notifications">♢ Thông báo</NavLink>
          <NavLink to="/settings">⚙ Cài đặt</NavLink>
          <div className="account">
            <span>NA</span>
            <div>
              <b>BS. Nguyen Van A</b>
              <small>minhanh@tampriviet.vn</small>
            </div>
          </div>
        </div>
      </aside>
      <div className="workspace">
        <header>
          <input placeholder="Tìm kiếm bệnh nhân, hồ sơ..." />
          <button>
            ♢<em />
          </button>
          <div className="mini-avatar">NA</div>
        </header>
        <main>{children}</main>
      </div>
    </div>
  );
}
function Title({ title, text, action }) {
  return (
    <div className="title">
      <div>
        <h1>{title}</h1>
        <p>{text}</p>
      </div>
      {action}
    </div>
  );
}
function Dashboard() {
  return (
    <Shell>
      <Title
        title="Chào buổi sáng, BS. Nguyễn Văn A!"
        text="Đây là tổng quan hoạt động của bạn hôm nay."
        action={<button className="primary">＋ Tạo lịch trống</button>}
      />
      <div className="stats">
        <Stat icon="▣" n="08" text="Lịch hẹn hôm nay" trend="2 lịch sắp tới" />
        <Stat icon="◫" n="12" text="Yêu cầu chờ duyệt" trend="+3 từ hôm qua" />
        <Stat icon="♙" n="124" text="Tổng bệnh nhân" trend="+8 tháng này" />
        <Stat
          icon="★"
          n="4.9"
          text="Đánh giá trung bình"
          trend="98% hài lòng"
        />
      </div>
      <div className="dashboard-grid">
        <section className="panel">
          <div className="panel-head">
            <h2>Lịch hẹn hôm nay</h2>
            <Link to="/calendar">Xem lịch →</Link>
          </div>
          {requests.slice(0, 2).map((r, i) => (
            <Appointment key={r[0]} item={r} time={i ? "14:30" : "09:00"} />
          ))}
        </section>
        <section className="panel">
          <div className="panel-head">
            <h2>Yêu cầu mới</h2>
            <Link to="/requests">Xem tất cả →</Link>
          </div>
          {requests.map((r) => (
            <div className="request-mini" key={r[0]}>
              <div className="avatar">{r[0].split(" ").at(-1)[0]}</div>
              <div>
                <b>{r[0]}</b>
                <small>{r[1]}</small>
              </div>
              <span>{r[3]}</span>
            </div>
          ))}
        </section>
      </div>
      <section className="panel chart">
        <div className="panel-head">
          <h2>Hoạt động tham vấn</h2>
          <select>
            <option>7 ngày qua</option>
          </select>
        </div>
        <div className="bars">
          {[45, 70, 55, 85, 65, 90, 60].map((x, i) => (
            <div key={i}>
              <i style={{ height: x + "%" }} />
              <small>{["T2", "T3", "T4", "T5", "T6", "T7", "CN"][i]}</small>
            </div>
          ))}
        </div>
      </section>
    </Shell>
  );
}
function Stat({ icon, n, text, trend }) {
  return (
    <div className="stat">
      <span>{icon}</span>
      <div>
        <b>{n}</b>
        <p>{text}</p>
        <small>{trend}</small>
      </div>
    </div>
  );
}
function Appointment({ item, time }) {
  return (
    <div className="appointment">
      <div className="time">
        <b>{time}</b>
        <small>60 phút</small>
      </div>
      <div className="avatar">{item[0].at(-1)}</div>
      <div>
        <b>{item[0]}</b>
        <small>{item[1]} · Trực tuyến</small>
      </div>
      <button>Vào phòng</button>
    </div>
  );
}
function Calendar() {
  const days = [20, 21, 22, 23, 24, 25, 26];
  return (
    <Shell>
      <Title
        title="Lịch làm việc"
        text="Quản lý thời gian làm việc và lịch hẹn của bạn."
        action={<button className="primary">＋ Thêm lịch trống</button>}
      />
      <div className="calendar-tools">
        <button>‹</button>
        <button>Hôm nay</button>
        <button>›</button>
        <h2>Tháng 7, 2026</h2>
        <div />
        <button>Tuần</button>
        <button>Tháng</button>
      </div>
      <section className="week-calendar">
        <div className="hours">
          {[
            "08:00",
            "09:00",
            "10:00",
            "11:00",
            "12:00",
            "13:00",
            "14:00",
            "15:00",
            "16:00",
            "17:00",
          ].map((x) => (
            <span key={x}>{x}</span>
          ))}
        </div>
        {days.map((d, i) => (
          <div className="day" key={d}>
            <b>
              {["T2", "T3", "T4", "T5", "T6", "T7", "CN"][i]}
              <strong>{d}</strong>
            </b>
            {i === 0 && <Event top={75} text="Nguyễn Thảo Vy" />}
            {i === 1 && <Event top={225} text="Trần Quốc Bảo" />}
            {i === 3 && <Event top={150} text="Lê Mai Anh" />}
          </div>
        ))}
      </section>
    </Shell>
  );
}
function Event({ top, text }) {
  return (
    <Link to="/patients/1" className="event" style={{ top }}>
      <b>{text}</b>
      <small>Tham vấn online</small>
    </Link>
  );
}
function Requests() {
  const [tab, setTab] = useState("Tất cả");
  return (
    <Shell>
      <Title
        title="Yêu cầu đặt lịch"
        text="Xem xét và phản hồi các yêu cầu tham vấn từ bệnh nhân."
      />
      <div className="tabs">
        {["Tất cả", "Chờ xác nhận", "Đã xác nhận", "Đã từ chối"].map((x) => (
          <button
            className={tab === x ? "active" : ""}
            onClick={() => setTab(x)}
            key={x}
          >
            {x}
          </button>
        ))}
      </div>
      <section className="panel table">
        <div className="table-head">
          <b>Bệnh nhân</b>
          <b>Vấn đề cần hỗ trợ</b>
          <b>Thời gian</b>
          <b>Trạng thái</b>
          <b>Thao tác</b>
        </div>
        {requests.map((r) => (
          <div className="table-row" key={r[0]}>
            <div className="patient">
              <div className="avatar">{r[0].at(-1)}</div>
              <div>
                <b>{r[0]}</b>
                <small>patient@email.com</small>
              </div>
            </div>
            <span>{r[1]}</span>
            <span>{r[2]}</span>
            <span className={r[3] === "Mới" ? "status new" : "status"}>
              {r[3]}
            </span>
            <div className="actions">
              <button>✓</button>
              <button>×</button>
              <Link to="/patients/1">›</Link>
            </div>
          </div>
        ))}
      </section>
    </Shell>
  );
}
function Profile() {
  return (
    <Shell>
      <Title
        title="Hồ sơ chuyên gia"
        text="Quản lý thông tin hiển thị với bệnh nhân."
        action={<button className="primary">Lưu thay đổi</button>}
      />
      <div className="profile-cover">
        <div className="big-avatar">NA</div>
        <div>
          <h2>BS. Nguyễn Văn A</h2>
          <p>Tâm lý học lâm sàng · TP. Hồ Chí Minh</p>
          <span>● Hồ sơ đã xác minh</span>
        </div>
        <button>Đổi ảnh đại diện</button>
      </div>
      <div className="profile-layout">
        <nav className="profile-nav">
          <a>Thông tin cơ bản</a>
          <a>Giới thiệu</a>
          <a>Chuyên môn</a>
          <a>Học vấn & Chứng chỉ</a>
          <a>Kinh nghiệm</a>
          <a>Chi phí tham vấn</a>
        </nav>
        <section className="panel form">
          <h2>Thông tin cơ bản</h2>
          <div className="form-grid">
            <Field label="Họ và tên" value="Nguyễn Văn A" />
            <Field label="Chức danh" value="Bác sĩ" />
            <Field label="Email" value="minhanh@tampriviet.vn" />
            <Field label="Số điện thoại" value="090 123 4567" />
          </div>
          <Field
            label="Địa chỉ làm việc"
            value="123 Nguyễn Đình Chiểu, Quận 3, TP.HCM"
          />
          <h2>Giới thiệu bản thân</h2>
          <textarea defaultValue="Tôi có hơn 10 năm kinh nghiệm trong lĩnh vực tâm lý học lâm sàng, đồng hành cùng thân chủ vượt qua lo âu, căng thẳng và khó khăn trong các mối quan hệ." />
          <h2>Chuyên môn</h2>
          <div className="chips">
            <span>Lo âu ×</span>
            <span>Trầm cảm ×</span>
            <span>Căng thẳng ×</span>
            <button>＋ Thêm</button>
          </div>
        </section>
      </div>
    </Shell>
  );
}
function Field({ label, value }) {
  return (
    <label>
      <span>{label}</span>
      <input defaultValue={value} />
    </label>
  );
}
function Patient() {
  return (
    <Shell>
      <Link to="/requests" className="back">
        ← Quay lại
      </Link>
      <div className="patient-hero">
        <div className="big-avatar">TV</div>
        <div>
          <h1>Nguyễn Thảo Vy</h1>
          <p>25 tuổi · Nữ · TP. Hồ Chí Minh</p>
          <span>Đã tham vấn 4 buổi</span>
        </div>
        <button className="primary">＋ Tạo ghi chú</button>
      </div>
      <div className="patient-grid">
        <section>
          <article className="panel">
            <h2>Thông tin tổng quan</h2>
            <div className="info-grid">
              <p>
                <small>Email</small>
                <b>thaovy@example.com</b>
              </p>
              <p>
                <small>Số điện thoại</small>
                <b>090 456 7890</b>
              </p>
              <p>
                <small>Ngày sinh</small>
                <b>15/08/2001</b>
              </p>
              <p>
                <small>Nghề nghiệp</small>
                <b>Nhân viên văn phòng</b>
              </p>
            </div>
          </article>
          <article className="panel">
            <h2>Lịch sử tham vấn</h2>
            {["15/07/2026", "08/07/2026", "01/07/2026"].map((x, i) => (
              <div className="history" key={x}>
                <span>✓</span>
                <div>
                  <b>Buổi tham vấn #{4 - i}</b>
                  <small>{x} · 60 phút · Trực tuyến</small>
                </div>
                <button>Xem ghi chú</button>
              </div>
            ))}
          </article>
        </section>
        <aside className="panel">
          <h2>Ghi chú chuyên môn</h2>
          <p>
            Thân chủ có dấu hiệu lo âu liên quan đến áp lực công việc. Khả năng
            nhận diện cảm xúc tốt và hợp tác tích cực.
          </p>
          <small>Cập nhật 15/07/2026</small>
          <h2>Bài đánh giá gần nhất</h2>
          <div className="score">
            <b>DASS-21</b>
            <strong>18</strong>
            <span>Lo âu mức vừa</span>
          </div>
        </aside>
      </div>
    </Shell>
  );
}
function Registration() {
  const [step, setStep] = useState(1);
  return (
    <div className="registration">
      <div className="reg-head">
        <Logo />
        <span>
          Đã có tài khoản? <b>Đăng nhập</b>
        </span>
      </div>
      <div className="steps">
        {[1, 2, 3, 4, 5].map((n) => (
          <div className={step >= n ? "done" : ""} key={n}>
            <i>{step > n ? "✓" : n}</i>
            <span>
              {
                ["Cá nhân", "Chuyên môn", "Học vấn", "Xác minh", "Cam kết"][
                  n - 1
                ]
              }
            </span>
          </div>
        ))}
      </div>
      <section className="reg-card">
        <span className="step-label">BƯỚC {step} / 5</span>
        <h1>
          {
            [
              "Thông tin cá nhân",
              "Thông tin chuyên môn",
              "Học vấn và kinh nghiệm",
              "Xác minh danh tính",
              "Cam kết thông tin",
            ][step - 1]
          }
        </h1>
        <p>
          Hoàn thiện thông tin để MindCare có thể xác minh hồ sơ chuyên gia của
          bạn.
        </p>
        {step === 1 && (
          <div className="form-grid">
            <Field label="Họ và tên *" />
            <Field label="Ngày sinh *" />
            <Field label="Số điện thoại *" />
            <Field label="Email *" />
          </div>
        )}
        {step === 2 && (
          <>
            <Field label="Chức danh chuyên môn *" />
            <Field label="Lĩnh vực chuyên môn *" />
            <label>
              <span>Giới thiệu bản thân *</span>
              <textarea />
            </label>
          </>
        )}
        {step === 3 && (
          <>
            <Field label="Trường đào tạo *" />
            <Field label="Chuyên ngành *" />
            <div className="upload">
              ⇧<b>Tải lên bằng cấp, chứng chỉ</b>
              <small>PDF, PNG hoặc JPG, tối đa 10MB</small>
            </div>
          </>
        )}
        {step === 4 && (
          <>
            <h3>CCCD/CMND *</h3>
            <div className="uploads">
              <div className="upload">
                ▣<b>Mặt trước</b>
              </div>
              <div className="upload">
                ▣<b>Mặt sau</b>
              </div>
            </div>
            <div className="upload">
              ♙<b>Ảnh chân dung rõ mặt</b>
            </div>
          </>
        )}
        {step === 5 && (
          <div className="commit">
            <label>
              <input type="checkbox" /> Tôi cam kết các thông tin cung cấp là
              chính xác.
            </label>
            <label>
              <input type="checkbox" /> Tôi đồng ý với Điều khoản sử dụng và
              Chính sách bảo mật.
            </label>
          </div>
        )}
        <div className="reg-actions">
          <button disabled={step === 1} onClick={() => setStep(step - 1)}>
            ← Quay lại
          </button>
          <button
            className="primary"
            onClick={() => setStep(Math.min(5, step + 1))}
          >
            {step === 5 ? "Gửi hồ sơ xét duyệt" : "Tiếp tục →"}
          </button>
        </div>
      </section>
    </div>
  );
}
function Placeholder({ title }) {
  return (
    <Shell>
      <Title
        title={title}
        text="Tính năng đang được đồng bộ với hệ thống MindCare."
      />
      <div className="panel empty">
        ◎<h2>{title}</h2>
        <p>Dữ liệu sẽ hiển thị khi backend tương ứng sẵn sàng.</p>
      </div>
    </Shell>
  );
}
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/calendar" element={<Calendar />} />
      <Route path="/requests" element={<Requests />} />
      <Route path="/profile" element={<Profile />} />
      <Route path="/patients/:id" element={<Patient />} />
      <Route path="/register" element={<Registration />} />
      <Route path="/statistics" element={<Placeholder title="Thống kê" />} />
      <Route
        path="/notifications"
        element={<Placeholder title="Thông báo" />}
      />
      <Route path="/settings" element={<Placeholder title="Cài đặt" />} />
    </Routes>
  );
}
